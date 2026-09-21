package br.com.govalue.repository;

import br.com.govalue.domain.AvaliacaoFuncionario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoFuncionarioRepository extends JpaRepository<AvaliacaoFuncionario, Long> {

    boolean existsByAvaliacaoId(Long avaliacaoId);

    boolean existsByPerguntaId(Long perguntaId);
}
