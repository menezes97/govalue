package br.com.govalue.repository;

import br.com.govalue.domain.AvaliacaoFuncionario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvaliacaoFuncionarioRepository extends JpaRepository<AvaliacaoFuncionario, Long> {

    boolean existsByAvaliacaoId(Long avaliacaoId);

    boolean existsByPerguntaId(Long perguntaId);

    boolean existsByFuncionarioIdAndAvaliacaoId(Long funcionarioId, Long avaliacaoId);

    String JOINS = """
            join fetch af.avaliacao a
            join fetch a.tipoAvaliacao
            join fetch af.pergunta p
            join fetch p.padraoResposta
            join fetch af.funcionario f
            left join fetch af.resposta
            left join fetch af.respostaGestor
            """;

    /** Todas as perguntas vinculadas a um funcionario (todas as avaliacoes). */
    @Query("select af from AvaliacaoFuncionario af " + JOINS + " where f.id = :funcionarioId order by a.id, p.id")
    List<AvaliacaoFuncionario> findDoFuncionario(@Param("funcionarioId") Long funcionarioId);

    /** Perguntas de uma avaliacao vinculadas a um funcionario. */
    @Query("select af from AvaliacaoFuncionario af " + JOINS
            + " where f.id = :funcionarioId and a.id = :avaliacaoId order by p.id")
    List<AvaliacaoFuncionario> findDoFuncionarioNaAvaliacao(
            @Param("funcionarioId") Long funcionarioId, @Param("avaliacaoId") Long avaliacaoId);

    /** Perguntas vinculadas aos subordinados diretos de um gestor. */
    @Query("select af from AvaliacaoFuncionario af " + JOINS
            + " where f.gestor.id = :gestorId order by f.nome, a.id, p.id")
    List<AvaliacaoFuncionario> findDosSubordinados(@Param("gestorId") Long gestorId);
}
