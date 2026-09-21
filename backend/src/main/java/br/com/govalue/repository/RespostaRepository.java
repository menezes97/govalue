package br.com.govalue.repository;

import br.com.govalue.domain.Resposta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RespostaRepository extends JpaRepository<Resposta, Long> {

    List<Resposta> findByPadraoRespostaId(Long padraoRespostaId);
}
