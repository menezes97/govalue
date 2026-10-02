package br.com.govalue.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Contrato HTTP com o face-service (Python). Opaco para o resto do backend — nunca exposto
 * ao frontend; os campos espelham exatamente o JSON snake_case que o FastAPI espera/devolve. */
public final class FaceServiceDtos {

    private FaceServiceDtos() {}

    public record ExtrairEmbeddingRequest(@JsonProperty("imagem_base64") String imagemBase64) {}

    public record ExtrairEmbeddingResponse(List<Double> embedding) {}

    public record VerificarRequest(
            @JsonProperty("imagem_base64") String imagemBase64,
            @JsonProperty("embedding_referencia") List<Double> embeddingReferencia) {}

    public record VerificarResponse(boolean corresponde, double distancia) {}
}
