package br.com.govalue.web.error;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        var corpo = new ApiError(400, "Dados invalidos", campos, Instant.now());
        return ResponseEntity.badRequest().body(corpo);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    ResponseEntity<ApiError> credenciais(CredenciaisInvalidasException e) {
        return resposta(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> acessoNegado(AccessDeniedException e) {
        return resposta(HttpStatus.FORBIDDEN, "Acesso negado");
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ApiError> naoEncontrado(RecursoNaoEncontradoException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(NegocioException.class)
    ResponseEntity<ApiError> negocio(NegocioException e) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integridade(DataIntegrityViolationException e) {
        return resposta(HttpStatus.CONFLICT, "Operacao viola uma restricao de integridade (registro duplicado ou em uso)");
    }

    private ResponseEntity<ApiError> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), mensagem));
    }
}
