package br.com.govalue.service;

import br.com.govalue.domain.Avaliacao;
import br.com.govalue.domain.AvaliacaoFuncionario;
import br.com.govalue.domain.Funcionario;
import br.com.govalue.domain.Pergunta;
import br.com.govalue.domain.StatusFuncionario;
import br.com.govalue.repository.AvaliacaoFuncionarioRepository;
import br.com.govalue.repository.AvaliacaoRepository;
import br.com.govalue.repository.FuncionarioRepository;
import br.com.govalue.repository.PerguntaRepository;
import br.com.govalue.web.dto.FluxoDtos.VincularRequest;
import br.com.govalue.web.dto.FluxoDtos.VinculoResultado;
import br.com.govalue.web.error.NegocioException;
import br.com.govalue.web.error.RecursoNaoEncontradoException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Vincula uma avaliacao a funcionarios: uma linha por funcionario x pergunta, sem duplicar quem ja a possui. */
@Service
public class VinculoService {

    private final AvaliacaoRepository avaliacoes;
    private final PerguntaRepository perguntas;
    private final FuncionarioRepository funcionarios;
    private final AvaliacaoFuncionarioRepository vinculos;
    private final Clock clock;

    public VinculoService(
            AvaliacaoRepository avaliacoes,
            PerguntaRepository perguntas,
            FuncionarioRepository funcionarios,
            AvaliacaoFuncionarioRepository vinculos,
            Clock clock) {
        this.avaliacoes = avaliacoes;
        this.perguntas = perguntas;
        this.funcionarios = funcionarios;
        this.vinculos = vinculos;
        this.clock = clock;
    }

    @Transactional
    public VinculoResultado vincular(Long avaliacaoId, VincularRequest req) {
        Avaliacao avaliacao = avaliacoes.findById(avaliacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Avaliação", avaliacaoId));
        if (avaliacao.getDataFimVigencia().isBefore(LocalDate.now(clock))) {
            throw new NegocioException("A vigência desta avaliação já foi encerrada");
        }

        List<Pergunta> perguntasDaAvaliacao = perguntas.findByAvaliacaoId(avaliacaoId);
        if (perguntasDaAvaliacao.isEmpty()) {
            throw new NegocioException("A avaliação não possui perguntas para vincular");
        }

        int vinculados = 0;
        int jaVinculados = 0;
        for (Funcionario funcionario : alvo(req)) {
            if (vinculos.existsByFuncionarioIdAndAvaliacaoId(funcionario.getId(), avaliacaoId)) {
                jaVinculados++;
                continue;
            }
            List<AvaliacaoFuncionario> linhas = new ArrayList<>();
            for (Pergunta pergunta : perguntasDaAvaliacao) {
                AvaliacaoFuncionario linha = new AvaliacaoFuncionario();
                linha.setFuncionario(funcionario);
                linha.setAvaliacao(avaliacao);
                linha.setPergunta(pergunta);
                linhas.add(linha);
            }
            vinculos.saveAll(linhas);
            vinculados++;
        }
        return new VinculoResultado(avaliacaoId, perguntasDaAvaliacao.size(), vinculados, jaVinculados);
    }

    private List<Funcionario> alvo(VincularRequest req) {
        if (req == null || req.funcionarioIds() == null || req.funcionarioIds().isEmpty()) {
            return funcionarios.findByStatus(StatusFuncionario.ATIVO);
        }

        Set<Long> pedidos = new HashSet<>(req.funcionarioIds());
        List<Funcionario> encontrados = funcionarios.findAllById(pedidos);
        Set<Long> achados = new HashSet<>();
        encontrados.forEach(f -> achados.add(f.getId()));
        pedidos.removeAll(achados);
        if (!pedidos.isEmpty()) {
            throw new NegocioException("Funcionários não encontrados: " + pedidos);
        }
        if (encontrados.stream().anyMatch(f -> f.getStatus() != StatusFuncionario.ATIVO)) {
            throw new NegocioException("Só é possível vincular funcionários ativos");
        }
        return encontrados;
    }
}
