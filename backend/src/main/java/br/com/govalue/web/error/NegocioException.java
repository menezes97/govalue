package br.com.govalue.web.error;

/** Violacao de regra de negocio (mapeada para HTTP 422). */
public class NegocioException extends RuntimeException {

    public NegocioException(String mensagem) {
        super(mensagem);
    }
}
