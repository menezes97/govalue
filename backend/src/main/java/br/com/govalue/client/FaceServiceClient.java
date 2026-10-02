package br.com.govalue.client;

import br.com.govalue.client.dto.FaceServiceDtos.ExtrairEmbeddingRequest;
import br.com.govalue.client.dto.FaceServiceDtos.ExtrairEmbeddingResponse;
import br.com.govalue.client.dto.FaceServiceDtos.VerificarRequest;
import br.com.govalue.client.dto.FaceServiceDtos.VerificarResponse;
import br.com.govalue.web.error.RostoInvalidoException;
import br.com.govalue.web.error.ServicoFacialIndisponivelException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Unica porta de saida do backend pro face-service (Python). Java nunca interpreta o
 * embedding — so o repassa como JSON opaco, igual chega do banco. */
@Component
public class FaceServiceClient {

    private record ErroFaceService(String detail) {}

    private final RestClient restClient;

    public FaceServiceClient(RestClient faceServiceRestClient) {
        this.restClient = faceServiceRestClient;
    }

    public List<Double> extrairEmbedding(String imagemBase64) {
        try {
            ExtrairEmbeddingResponse resposta = restClient
                    .post()
                    .uri("/embeddings")
                    .body(new ExtrairEmbeddingRequest(imagemBase64))
                    .retrieve()
                    .body(ExtrairEmbeddingResponse.class);
            return resposta.embedding();
        } catch (RestClientResponseException e) {
            throw mapearErro(e);
        } catch (Exception e) {
            throw new ServicoFacialIndisponivelException(e);
        }
    }

    public VerificarResponse verificar(String imagemBase64, List<Double> embeddingReferencia) {
        try {
            return restClient
                    .post()
                    .uri("/verify")
                    .body(new VerificarRequest(imagemBase64, embeddingReferencia))
                    .retrieve()
                    .body(VerificarResponse.class);
        } catch (RestClientResponseException e) {
            throw mapearErro(e);
        } catch (Exception e) {
            throw new ServicoFacialIndisponivelException(e);
        }
    }

    private RuntimeException mapearErro(RestClientResponseException e) {
        if (e.getStatusCode().value() == 422) {
            String mensagem;
            try {
                mensagem = e.getResponseBodyAs(ErroFaceService.class).detail();
            } catch (Exception leituraFalhou) {
                mensagem = "Rosto não identificado na imagem";
            }
            return new RostoInvalidoException(mensagem);
        }
        return new ServicoFacialIndisponivelException(e);
    }
}
