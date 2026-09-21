package br.com.govalue.repository;

import br.com.govalue.domain.TipoAvaliacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoAvaliacaoRepository extends JpaRepository<TipoAvaliacao, Long> {
}
