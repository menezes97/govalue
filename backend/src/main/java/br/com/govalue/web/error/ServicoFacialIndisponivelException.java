package br.com.govalue.web.error;

/** O face-service nao respondeu (fora do ar, timeout). Mapeada para HTTP 503 — falha fechada:
 * em nenhuma hipotese um token completo e emitido se o segundo fator nao pode ser verificado. */
public class ServicoFacialIndisponivelException extends RuntimeException {

    public ServicoFacialIndisponivelException(Throwable causa) {
        super("Verificação facial indisponível no momento", causa);
    }
}
