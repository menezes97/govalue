package br.com.govalue.repository;

import br.com.govalue.domain.Funcionario;
import br.com.govalue.domain.StatusFuncionario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    Optional<Funcionario> findByUsuarioId(Long usuarioId);

    boolean existsByCpf(String cpf);

    boolean existsByGestorId(Long gestorId);

    List<Funcionario> findByGestorId(Long gestorId);

    List<Funcionario> findByNomeContainingIgnoreCaseOrderByNome(String nome);

    List<Funcionario> findByStatus(StatusFuncionario status);
}
