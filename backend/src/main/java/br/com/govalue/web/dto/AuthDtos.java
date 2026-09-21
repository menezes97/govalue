package br.com.govalue.web.dto;

import br.com.govalue.domain.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String senha) {}

    public record AlterarSenhaRequest(
            @NotBlank String senhaAtual, @NotBlank @Size(min = 8, max = 72) String novaSenha) {}

    public record UsuarioResponse(
            Long id, String nome, String email, Perfil perfil, Long funcionarioId, boolean gestor) {}

    public record LoginResponse(String token, Instant expiraEm, UsuarioResponse usuario) {}
}
