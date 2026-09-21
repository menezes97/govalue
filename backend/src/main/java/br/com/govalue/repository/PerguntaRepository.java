package br.com.govalue.repository;

import br.com.govalue.domain.Pergunta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerguntaRepository extends JpaRepository<Pergunta, Long> {

    List<Pergunta> findByAvaliacaoId(Long avaliacaoId);
}
