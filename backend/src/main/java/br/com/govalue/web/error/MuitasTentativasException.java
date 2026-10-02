package br.com.govalue.web.error;

/** Muitas tentativas de login falhas pro mesmo e-mail numa janela curta (mapeada para HTTP 429). */
public class MuitasTentativasException extends RuntimeException {

    public MuitasTentativasException() {
        super("Muitas tentativas. Tente novamente mais tarde.");
    }
}
