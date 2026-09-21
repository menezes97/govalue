package br.com.govalue.repository;

import br.com.govalue.domain.AvaliacaoFuncionario;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Agregacoes do dashboard feitas no banco (equivalentes as consultas de "codigo importante" do TCC). */
public interface DashboardRepository extends Repository<AvaliacaoFuncionario, Long> {

    interface ConclusaoRow {
        Long getFuncionarioId();

        String getNome();

        Long getTotal();

        Long getRespondidas();

        Long getRespondidasGestor();
    }

    /** respostaId nulo representa o balde "sem resposta". */
    interface ContagemRow {
        Long getPerguntaId();

        Long getRespostaId();

        Long getQuantidade();
    }

    @Query("""
            select f.id as funcionarioId, f.nome as nome, count(af) as total,
                   count(af.resposta) as respondidas, count(af.respostaGestor) as respondidasGestor
            from AvaliacaoFuncionario af join af.funcionario f
            where af.avaliacao.id = :avaliacaoId
            group by f.id, f.nome
            order by f.nome
            """)
    List<ConclusaoRow> conclusaoPorFuncionario(@Param("avaliacaoId") Long avaliacaoId);

    @Query("""
            select p.id as perguntaId, r.id as respostaId, count(af) as quantidade
            from AvaliacaoFuncionario af join af.pergunta p left join af.resposta r
            where af.avaliacao.id = :avaliacaoId
            group by p.id, r.id
            """)
    List<ContagemRow> contagemRespostasDoFuncionario(@Param("avaliacaoId") Long avaliacaoId);

    @Query("""
            select p.id as perguntaId, r.id as respostaId, count(af) as quantidade
            from AvaliacaoFuncionario af join af.pergunta p left join af.respostaGestor r
            where af.avaliacao.id = :avaliacaoId
            group by p.id, r.id
            """)
    List<ContagemRow> contagemRespostasDoGestor(@Param("avaliacaoId") Long avaliacaoId);
}
