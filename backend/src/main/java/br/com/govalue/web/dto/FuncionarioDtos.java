package br.com.govalue.web.dto;

import br.com.govalue.domain.Funcionario;
import br.com.govalue.domain.StatusFuncionario;
import br.com.govalue.validation.Cpf;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class FuncionarioDtos {

    private FuncionarioDtos() {}

    /** Cria o funcionario e o usuario de acesso dele na mesma operacao. */
    public record CriarFuncionarioRequest(
            @NotBlank @Size(max = 150) String nome,
            @NotBlank @Cpf String cpf,
            @Size(max = 100) String funcao,
            @Size(max = 30) String matricula,
            @Size(max = 100) String area,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 72) String senhaInicial,
            Long gestorId) {}

    public record AtualizarFuncionarioRequest(
            @NotBlank @Size(max = 150) String nome,
            @NotBlank @Cpf String cpf,
            @Size(max = 100) String funcao,
            @Size(max = 30) String matricula,
            @Size(max = 100) String area,
            @NotBlank @Email @Size(max = 150) String email,
            @NotNull StatusFuncionario status,
            Long gestorId) {}

    public record FuncionarioResponse(
            Long id,
            String nome,
            String cpf,
            String funcao,
            String matricula,
            String area,
            String email,
            StatusFuncionario status,
            Long gestorId,
            String gestorNome) {

        public static FuncionarioResponse de(Funcionario f) {
            var gestor = f.getGestor();
            return new FuncionarioResponse(
                    f.getId(),
                    f.getNome(),
                    f.getCpf(),
                    f.getFuncao(),
                    f.getMatricula(),
                    f.getArea(),
                    f.getUsuario().getEmail(),
                    f.getStatus(),
                    gestor == null ? null : gestor.getId(),
                    gestor == null ? null : gestor.getNome());
        }
    }
}
