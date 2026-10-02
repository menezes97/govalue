package br.com.govalue.web.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public final class FaceDtos {

    private FaceDtos() {}

    /** Passo intermediario do login: senha OK, falta confirmar o rosto. Nao e erro — 200 OK. */
    public record LoginDesafioFacialResponse(String tokenFacePendente, Instant expiraEm) implements LoginResultado {}

    public record FaceLoginRequest(@NotBlank String tokenFacePendente, @NotBlank String imagemBase64) {}

    public record RegistrarFaceRequest(@NotBlank String imagemBase64, boolean consentimento) {}

    public record VerificacaoFacialStatusResponse(boolean habilitada) {}
}
