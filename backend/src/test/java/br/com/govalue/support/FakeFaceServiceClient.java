package br.com.govalue.support;

import br.com.govalue.client.FaceServiceClient;
import br.com.govalue.client.dto.FaceServiceDtos.VerificarResponse;
import br.com.govalue.web.error.RostoInvalidoException;
import br.com.govalue.web.error.ServicoFacialIndisponivelException;
import java.util.List;
import org.springframework.web.client.RestClient;

/** Dublê de teste do FaceServiceClient — evita depender do microsserviço Python real nos testes
 * de integração. Comportamento controlado pelos próprios testes via os campos públicos. */
public class FakeFaceServiceClient extends FaceServiceClient {

    public boolean indisponivel = false;
    public boolean semRosto = false;
    public boolean corresponde = true;
    public List<Double> embeddingExtraido = List.of(1.0, 2.0, 3.0);

    public FakeFaceServiceClient() {
        super(RestClient.builder().build());
    }

    public void resetar() {
        indisponivel = false;
        semRosto = false;
        corresponde = true;
        embeddingExtraido = List.of(1.0, 2.0, 3.0);
    }

    @Override
    public List<Double> extrairEmbedding(String imagemBase64) {
        validarEstado();
        return embeddingExtraido;
    }

    @Override
    public VerificarResponse verificar(String imagemBase64, List<Double> embeddingReferencia) {
        validarEstado();
        return new VerificarResponse(corresponde, corresponde ? 0.3 : 0.9);
    }

    private void validarEstado() {
        if (indisponivel) {
            throw new ServicoFacialIndisponivelException(new RuntimeException("face-service indisponível (fake)"));
        }
        if (semRosto) {
            throw new RostoInvalidoException("Nenhum rosto detectado na imagem");
        }
    }
}
