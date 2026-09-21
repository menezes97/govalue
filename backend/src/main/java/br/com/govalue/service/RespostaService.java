package br.com.govalue.service;

import br.com.govalue.domain.Avaliacao;
import br.com.govalue.domain.AvaliacaoFuncionario;
import br.com.govalue.domain.Funcionario;
import br.com.govalue.domain.Pergunta;
import br.com.govalue.domain.Resposta;
import br.com.govalue.repository.AvaliacaoFuncionarioRepository;
import br.com.govalue.repository.FuncionarioRepository;
import br.com.govalue.repository.RespostaRepository;
import br.com.govalue.security.UsuarioAutenticado;
import br.com.govalue.web.dto.FluxoDtos.DetalheAvaliacao;
import br.com.govalue.web.dto.FluxoDtos.ItemResposta;
import br.com.govalue.web.dto.FluxoDtos.OpcaoResposta;
import br.com.govalue.web.dto.FluxoDtos.PendenciaGestor;
import br.com.govalue.web.dto.FluxoDtos.PerguntaRespondivel;
import br.com.govalue.web.dto.FluxoDtos.ResumoAvaliacao;
import br.com.govalue.web.dto.FluxoDtos.SalvarRespostasRequest;
import br.com.govalue.web.error.NegocioException;
import br.com.govalue.web.error.RecursoNaoEncontradoException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fluxo de resposta: o funcionario responde a propria avaliacao e o gestor responde sobre os subordinados diretos. */
@Service
public class RespostaService {

    private final FuncionarioRepository funcionarios;
    private final AvaliacaoFuncionarioRepository vinculos;
    private final RespostaRepository respostas;
    private final Clock clock;

    public RespostaService(
            FuncionarioRepository funcionarios,
            AvaliacaoFuncionarioRepository vinculos,
            RespostaRepository respostas,
            Clock clock) {
        this.funcionarios = funcionarios;
        this.vinculos = vinculos;
        this.respostas = respostas;
        this.clock = clock;
    }

    // ---------- funcionario ----------

    @Transactional(readOnly = true)
    public List<ResumoAvaliacao> minhasAvaliacoes(UsuarioAutenticado usuario) {
        Funcionario funcionario = funcionarioDe(usuario);
        LocalDate hoje = LocalDate.now(clock);
        return agruparPor(vinculos.findDoFuncionario(funcionario.getId()), l -> l.getAvaliacao().getId())
                .values()
                .stream()
                .map(linhas -> {
                    Avaliacao a = linhas.getFirst().getAvaliacao();
                    int respondidas = (int) linhas.stream().filter(l -> l.getResposta() != null).count();
                    return new ResumoAvaliacao(
                            a.getId(),
                            a.getDescricao(),
                            a.getTipoAvaliacao().getDescricao(),
                            a.getDataInicioVigencia(),
                            a.getDataFimVigencia(),
                            a.abertaEm(hoje),
                            linhas.size(),
                            respondidas);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public DetalheAvaliacao minhaAvaliacao(UsuarioAutenticado usuario, Long avaliacaoId) {
        Funcionario funcionario = funcionarioDe(usuario);
        return detalhe(linhasDe(funcionario.getId(), avaliacaoId), funcionario, false);
    }

    @Transactional
    public DetalheAvaliacao salvarMinhasRespostas(
            UsuarioAutenticado usuario, Long avaliacaoId, SalvarRespostasRequest req) {
        Funcionario funcionario = funcionarioDe(usuario);
        List<AvaliacaoFuncionario> linhas = linhasDe(funcionario.getId(), avaliacaoId);
        aplicar(linhas, req, false);
        return detalhe(linhas, funcionario, false);
    }

    // ---------- gestor ----------

    @Transactional(readOnly = true)
    public List<PendenciaGestor> avaliacoesDosSubordinados(UsuarioAutenticado usuario) {
        Funcionario gestor = gestorDe(usuario);
        LocalDate hoje = LocalDate.now(clock);

        record Chave(Long funcionarioId, Long avaliacaoId) {}
        return agruparPor(
                        vinculos.findDosSubordinados(gestor.getId()),
                        l -> new Chave(l.getFuncionario().getId(), l.getAvaliacao().getId()))
                .values()
                .stream()
                .map(linhas -> {
                    AvaliacaoFuncionario primeira = linhas.getFirst();
                    Avaliacao a = primeira.getAvaliacao();
                    int respondidas = (int) linhas.stream().filter(l -> l.getRespostaGestor() != null).count();
                    return new PendenciaGestor(
                            primeira.getFuncionario().getId(),
                            primeira.getFuncionario().getNome(),
                            a.getId(),
                            a.getDescricao(),
                            a.getDataFimVigencia(),
                            a.abertaEm(hoje),
                            linhas.size(),
                            respondidas);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public DetalheAvaliacao avaliacaoDoSubordinado(UsuarioAutenticado usuario, Long avaliacaoId, Long funcionarioId) {
        Funcionario subordinado = subordinadoDe(gestorDe(usuario), funcionarioId);
        return detalhe(linhasDe(subordinado.getId(), avaliacaoId), subordinado, true);
    }

    @Transactional
    public DetalheAvaliacao salvarRespostasDoGestor(
            UsuarioAutenticado usuario, Long avaliacaoId, Long funcionarioId, SalvarRespostasRequest req) {
        Funcionario subordinado = subordinadoDe(gestorDe(usuario), funcionarioId);
        List<AvaliacaoFuncionario> linhas = linhasDe(subordinado.getId(), avaliacaoId);
        aplicar(linhas, req, true);
        return detalhe(linhas, subordinado, true);
    }

    // ---------- regras compartilhadas ----------

    /** Valida tudo antes de gravar; qualquer violacao desfaz a transacao inteira (nada e salvo pela metade). */
    private void aplicar(List<AvaliacaoFuncionario> linhas, SalvarRespostasRequest req, boolean comoGestor) {
        Avaliacao avaliacao = linhas.getFirst().getAvaliacao();
        if (!avaliacao.abertaEm(LocalDate.now(clock))) {
            throw new NegocioException("A avaliação está fora do período de vigência e não pode ser alterada");
        }

        Map<Long, AvaliacaoFuncionario> porPergunta =
                linhas.stream().collect(Collectors.toMap(l -> l.getPergunta().getId(), Function.identity()));
        Set<Long> jaEnviadas = new HashSet<>();

        for (ItemResposta item : req.respostas()) {
            if (!jaEnviadas.add(item.perguntaId())) {
                throw new NegocioException("Pergunta repetida na requisição: " + item.perguntaId());
            }
            AvaliacaoFuncionario linha = porPergunta.get(item.perguntaId());
            if (linha == null) {
                throw new NegocioException("A pergunta " + item.perguntaId() + " não pertence a esta avaliação");
            }
            Resposta resposta = respostas.findById(item.respostaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Resposta", item.respostaId()));
            Long padraoDaPergunta = linha.getPergunta().getPadraoResposta().getId();
            if (!resposta.getPadraoResposta().getId().equals(padraoDaPergunta)) {
                throw new NegocioException("A resposta " + item.respostaId()
                        + " não é uma opção válida para a pergunta " + item.perguntaId());
            }
            if (comoGestor) {
                linha.setRespostaGestor(resposta);
            } else {
                linha.setResposta(resposta);
            }
        }
    }

    private DetalheAvaliacao detalhe(List<AvaliacaoFuncionario> linhas, Funcionario dono, boolean visaoGestor) {
        Avaliacao a = linhas.getFirst().getAvaliacao();
        Map<Long, List<OpcaoResposta>> opcoesPorPadrao = new HashMap<>();

        List<PerguntaRespondivel> perguntas = linhas.stream()
                .map(l -> {
                    Pergunta p = l.getPergunta();
                    List<OpcaoResposta> opcoes = opcoesPorPadrao.computeIfAbsent(
                            p.getPadraoResposta().getId(),
                            padraoId -> respostas.findByPadraoRespostaId(padraoId).stream()
                                    .map(r -> new OpcaoResposta(r.getId(), r.getDescricao()))
                                    .toList());
                    return new PerguntaRespondivel(
                            p.getId(),
                            p.getDescricao(),
                            opcoes,
                            idDe(l.getResposta()),
                            visaoGestor ? idDe(l.getRespostaGestor()) : null);
                })
                .toList();

        return new DetalheAvaliacao(
                a.getId(),
                a.getDescricao(),
                a.getTipoAvaliacao().getDescricao(),
                a.getDataInicioVigencia(),
                a.getDataFimVigencia(),
                a.abertaEm(LocalDate.now(clock)),
                dono.getId(),
                dono.getNome(),
                perguntas);
    }

    private List<AvaliacaoFuncionario> linhasDe(Long funcionarioId, Long avaliacaoId) {
        List<AvaliacaoFuncionario> linhas = vinculos.findDoFuncionarioNaAvaliacao(funcionarioId, avaliacaoId);
        if (linhas.isEmpty()) {
            throw new RecursoNaoEncontradoException("Avaliação vinculada", avaliacaoId);
        }
        return linhas;
    }

    private Funcionario funcionarioDe(UsuarioAutenticado usuario) {
        return funcionarios.findByUsuarioId(usuario.id())
                .orElseThrow(() -> new NegocioException("Seu usuário não possui cadastro de funcionário"));
    }

    /** "Gestor" nao e um perfil: e quem tem ao menos um subordinado direto. */
    private Funcionario gestorDe(UsuarioAutenticado usuario) {
        Funcionario gestor = funcionarioDe(usuario);
        if (!funcionarios.existsByGestorId(gestor.getId())) {
            throw new AccessDeniedException("Você não possui subordinados diretos");
        }
        return gestor;
    }

    private Funcionario subordinadoDe(Funcionario gestor, Long funcionarioId) {
        Funcionario funcionario = funcionarios.findById(funcionarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário", funcionarioId));
        if (funcionario.getGestor() == null || !funcionario.getGestor().getId().equals(gestor.getId())) {
            throw new AccessDeniedException("Este funcionário não é seu subordinado direto");
        }
        return funcionario;
    }

    private static Long idDe(Resposta resposta) {
        return resposta == null ? null : resposta.getId();
    }

    private static <K> Map<K, List<AvaliacaoFuncionario>> agruparPor(
            List<AvaliacaoFuncionario> linhas, Function<AvaliacaoFuncionario, K> chave) {
        return linhas.stream().collect(Collectors.groupingBy(chave, LinkedHashMap::new, Collectors.toList()));
    }
}
