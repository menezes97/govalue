package br.com.govalue.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** DTOs do fluxo de avaliacao: vinculo (admin), resposta do funcionario e resposta do gestor. */
public final class FluxoDtos {

    private FluxoDtos() {}

    /** Sem funcionarioIds (ou lista vazia), a avaliacao e vinculada a todos os funcionarios ativos. */
    public record VincularRequest(List<Long> funcionarioIds) {}

    public record VinculoResultado(
            Long avaliacaoId, int totalPerguntas, int funcionariosVinculados, int funcionariosJaVinculados) {}

    public record ItemResposta(@NotNull Long perguntaId, @NotNull Long respostaId) {}

    public record SalvarRespostasRequest(@NotEmpty @Valid List<ItemResposta> respostas) {}

    public record OpcaoResposta(Long id, String descricao) {}

    /** respostaGestorId fica nulo na visao do funcionario (a opiniao do gestor nao e exposta a ele). */
    public record PerguntaRespondivel(
            Long perguntaId,
            String descricao,
            List<OpcaoResposta> opcoes,
            Long respostaFuncionarioId,
            Long respostaGestorId) {}

    public record DetalheAvaliacao(
            Long avaliacaoId,
            String descricao,
            String tipo,
            LocalDate dataInicioVigencia,
            LocalDate dataFimVigencia,
            boolean aberta,
            Long funcionarioId,
            String funcionarioNome,
            List<PerguntaRespondivel> perguntas) {}

    public record ResumoAvaliacao(
            Long avaliacaoId,
            String descricao,
            String tipo,
            LocalDate dataInicioVigencia,
            LocalDate dataFimVigencia,
            boolean aberta,
            int totalPerguntas,
            int respondidas) {}

    public record PendenciaGestor(
            Long funcionarioId,
            String funcionarioNome,
            Long avaliacaoId,
            String descricao,
            LocalDate dataFimVigencia,
            boolean aberta,
            int totalPerguntas,
            int respondidasPeloGestor) {}
}
