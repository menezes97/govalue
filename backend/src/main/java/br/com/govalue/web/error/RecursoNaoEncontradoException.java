package br.com.govalue.web.error;

/** Recurso inexistente (mapeada para HTTP 404). */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Object id) {
        super(recurso + " não encontrado(a): " + id);
    }
}
