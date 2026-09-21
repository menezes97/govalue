package br.com.govalue.service;

import br.com.govalue.domain.Avaliacao;
import br.com.govalue.domain.Pergunta;
import br.com.govalue.domain.Resposta;
import br.com.govalue.repository.AvaliacaoRepository;
import br.com.govalue.repository.DashboardRepository;
import br.com.govalue.repository.DashboardRepository.ConclusaoRow;
import br.com.govalue.repository.DashboardRepository.ContagemRow;
import br.com.govalue.repository.PerguntaRepository;
import br.com.govalue.repository.RespostaRepository;
import br.com.govalue.web.dto.DashboardDtos.Conclusao;
import br.com.govalue.web.dto.DashboardDtos.ConclusaoFuncionario;
import br.com.govalue.web.dto.DashboardDtos.OpcaoResultado;
import br.com.govalue.web.dto.DashboardDtos.ResultadoAvaliacao;
import br.com.govalue.web.dto.DashboardDtos.ResultadoPergunta;
import br.com.govalue.web.dto.DashboardDtos.Visao;
import br.com.govalue.web.error.RecursoNaoEncontradoException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final AvaliacaoRepository avaliacoes;
    private final PerguntaRepository perguntas;
    private final RespostaRepository respostas;
    private final DashboardRepository dashboard;

    public DashboardService(
            AvaliacaoRepository avaliacoes,
            PerguntaRepository perguntas,
            RespostaRepository respostas,
            DashboardRepository dashboard) {
        this.avaliacoes = avaliacoes;
        this.perguntas = perguntas;
        this.respostas = respostas;
        this.dashboard = dashboard;
    }

    /** % de conclusao por funcionario e geral (mesma conta do original: respondidas / total, arredondado). */
    @Transactional(readOnly = true)
    public Conclusao conclusao(Long avaliacaoId) {
        Avaliacao avaliacao = obter(avaliacaoId);
        List<ConclusaoRow> linhas = dashboard.conclusaoPorFuncionario(avaliacaoId);

        List<ConclusaoFuncionario> funcionarios = linhas.stream()
                .map(l -> new ConclusaoFuncionario(
                        l.getFuncionarioId(),
                        l.getNome(),
                        l.getTotal().intValue(),
                        l.getRespondidas().intValue(),
                        inteiro(l.getRespondidas(), l.getTotal()),
                        l.getRespondidasGestor().intValue(),
                        inteiro(l.getRespondidasGestor(), l.getTotal())))
                .toList();

        long total = linhas.stream().mapToLong(ConclusaoRow::getTotal).sum();
        long respondidas = linhas.stream().mapToLong(ConclusaoRow::getRespondidas).sum();
        int concluidos = (int) funcionarios.stream().filter(f -> f.percentual() == 100).count();
        return new Conclusao(
                avaliacaoId,
                avaliacao.getDescricao(),
                funcionarios.size(),
                concluidos,
                decimal(respondidas, total),
                funcionarios);
    }

    /** Distribuicao das respostas por pergunta, incluindo opcoes sem nenhuma resposta e o balde "sem resposta". */
    @Transactional(readOnly = true)
    public ResultadoAvaliacao resultados(Long avaliacaoId, Visao visao) {
        Avaliacao avaliacao = obter(avaliacaoId);

        List<ContagemRow> contagens = visao == Visao.GESTOR
                ? dashboard.contagemRespostasDoGestor(avaliacaoId)
                : dashboard.contagemRespostasDoFuncionario(avaliacaoId);

        // (perguntaId -> (respostaId, nulo = sem resposta -> quantidade))
        Map<Long, Map<Long, Long>> porPergunta = new HashMap<>();
        for (ContagemRow c : contagens) {
            porPergunta.computeIfAbsent(c.getPerguntaId(), k -> new HashMap<>()).put(c.getRespostaId(), c.getQuantidade());
        }

        Map<Long, List<Resposta>> opcoesPorPadrao = new HashMap<>();
        List<ResultadoPergunta> resultado = perguntas.findByAvaliacaoId(avaliacaoId).stream()
                .sorted(Comparator.comparing(Pergunta::getId))
                .map(p -> {
                    List<Resposta> opcoes = opcoesPorPadrao.computeIfAbsent(
                            p.getPadraoResposta().getId(),
                            id -> respostas.findByPadraoRespostaId(id).stream()
                                    .sorted(Comparator.comparing(Resposta::getId))
                                    .toList());
                    // HashMap (e nao Map.of): a chave nula representa "sem resposta" e Map.of() nao aceita nulo.
                    return montar(p, opcoes, porPergunta.getOrDefault(p.getId(), new HashMap<>()));
                })
                .toList();

        return new ResultadoAvaliacao(avaliacaoId, avaliacao.getDescricao(), visao, resultado);
    }

    private ResultadoPergunta montar(Pergunta pergunta, List<Resposta> opcoes, Map<Long, Long> contagem) {
        long total = contagem.values().stream().mapToLong(Long::longValue).sum();
        long semResposta = contagem.getOrDefault(null, 0L);

        List<OpcaoResultado> opcoesResultado = opcoes.stream()
                .map(o -> {
                    long qtd = contagem.getOrDefault(o.getId(), 0L);
                    return new OpcaoResultado(o.getId(), o.getDescricao(), (int) qtd, decimal(qtd, total));
                })
                .toList();

        return new ResultadoPergunta(
                pergunta.getId(),
                pergunta.getDescricao(),
                (int) total,
                (int) (total - semResposta),
                (int) semResposta,
                decimal(semResposta, total),
                opcoesResultado);
    }

    private Avaliacao obter(Long id) {
        return avaliacoes.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Avaliação", id));
    }

    private static int inteiro(long parte, long total) {
        return total == 0 ? 0 : (int) Math.round(parte * 100.0 / total);
    }

    /** Percentual com uma casa decimal; sem vinculos devolve 0 em vez de dividir por zero. */
    private static double decimal(long parte, long total) {
        return total == 0 ? 0.0 : Math.round(parte * 1000.0 / total) / 10.0;
    }
}
