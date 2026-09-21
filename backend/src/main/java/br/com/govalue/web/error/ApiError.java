package br.com.govalue.web.error;

import java.time.Instant;
import java.util.Map;

public record ApiError(int status, String mensagem, Map<String, String> campos, Instant timestamp) {

    public static ApiError of(int status, String mensagem) {
        return new ApiError(status, mensagem, Map.of(), Instant.now());
    }
}
