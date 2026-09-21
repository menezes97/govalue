package br.com.govalue.web.dto;

import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {}

    /** Qual resposta compor no resultado: a do proprio funcionario ou a do gestor sobre ele. */
    public enum Visao {
        FUNCIONARIO,
        GESTOR
    }

    public record ConclusaoFuncionario(
            Long funcionarioId,
            String nome,
            int totalPerguntas,
            int respondidas,
            int percentual,
            int respondidasPeloGestor,
            int percentualGestor) {}

    public record Conclusao(
            Long avaliacaoId,
            String descricao,
            int totalFuncionarios,
            int concluidos,
            double percentualGeral,
            List<ConclusaoFuncionario> funcionarios) {}

    /** percentual sobre o total vinculado a pergunta (mesma base do dashboard original). */
    public record OpcaoResultado(Long respostaId, String descricao, int quantidade, double percentual) {}

    public record ResultadoPergunta(
            Long perguntaId,
            String descricao,
            int totalVinculados,
            int respondidas,
            int semResposta,
            double semRespostaPercentual,
            List<OpcaoResultado> opcoes) {}

    public record ResultadoAvaliacao(
            Long avaliacaoId, String descricao, Visao visao, List<ResultadoPergunta> perguntas) {}
}
