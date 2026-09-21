package br.com.govalue.web.error;

/** Login recusado (mapeada para HTTP 401). A mensagem e sempre a mesma para nao revelar se o e-mail existe. */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("E-mail ou senha inválidos");
    }
}
