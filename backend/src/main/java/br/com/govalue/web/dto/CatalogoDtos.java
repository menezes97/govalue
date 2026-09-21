package br.com.govalue.web.dto;

import br.com.govalue.domain.Avaliacao;
import br.com.govalue.domain.PadraoResposta;
import br.com.govalue.domain.Pergunta;
import br.com.govalue.domain.Resposta;
import br.com.govalue.domain.TipoAvaliacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public final class CatalogoDtos {

    private CatalogoDtos() {}

    public record TipoAvaliacaoResponse(Long id, String descricao) {
        public static TipoAvaliacaoResponse de(TipoAvaliacao t) {
            return new TipoAvaliacaoResponse(t.getId(), t.getDescricao());
        }
    }

    public record AvaliacaoRequest(
            @NotBlank @Size(max = 200) String descricao,
            @NotNull LocalDate dataInicioVigencia,
            @NotNull LocalDate dataFimVigencia,
            @NotNull Long tipoAvaliacaoId) {}

    public record AvaliacaoResponse(
            Long id,
            String descricao,
            LocalDate dataInicioVigencia,
            LocalDate dataFimVigencia,
            Long tipoAvaliacaoId,
            String tipoAvaliacao) {
        public static AvaliacaoResponse de(Avaliacao a) {
            return new AvaliacaoResponse(
                    a.getId(),
                    a.getDescricao(),
                    a.getDataInicioVigencia(),
                    a.getDataFimVigencia(),
                    a.getTipoAvaliacao().getId(),
                    a.getTipoAvaliacao().getDescricao());
        }
    }

    public record PerguntaRequest(
            @NotBlank @Size(max = 500) String descricao, @NotNull Long padraoRespostaId) {}

    public record PerguntaResponse(
            Long id, Long avaliacaoId, String descricao, Long padraoRespostaId, String padraoResposta) {
        public static PerguntaResponse de(Pergunta p) {
            return new PerguntaResponse(
                    p.getId(),
                    p.getAvaliacao().getId(),
                    p.getDescricao(),
                    p.getPadraoResposta().getId(),
                    p.getPadraoResposta().getDescricao());
        }
    }

    public record PadraoRespostaRequest(@NotBlank @Size(max = 100) String descricao) {}

    public record PadraoRespostaResponse(Long id, String descricao) {
        public static PadraoRespostaResponse de(PadraoResposta p) {
            return new PadraoRespostaResponse(p.getId(), p.getDescricao());
        }
    }

    public record RespostaRequest(@NotBlank @Size(max = 150) String descricao) {}

    public record RespostaResponse(Long id, Long padraoRespostaId, String descricao) {
        public static RespostaResponse de(Resposta r) {
            return new RespostaResponse(r.getId(), r.getPadraoResposta().getId(), r.getDescricao());
        }
    }
}
