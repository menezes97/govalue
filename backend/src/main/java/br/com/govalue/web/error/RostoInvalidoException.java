package br.com.govalue.web.error;

/** Foto enviada pro proprio usuario (registro ou verificacao) sem rosto detectavel, ou com mais
 * de um rosto (mapeada para HTTP 422). Aqui nao ha problema de vazamento de informacao — e o
 * proprio usuario fotografando a si mesmo. */
public class RostoInvalidoException extends RuntimeException {

    public RostoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
