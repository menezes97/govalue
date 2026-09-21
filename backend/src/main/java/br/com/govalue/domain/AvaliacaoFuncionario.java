package br.com.govalue.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Uma pergunta de uma avaliacao vinculada a um funcionario, com a resposta dele e a do gestor. */
@Entity
@Table(name = "avaliacao_funcionario")
@Getter
@Setter
@NoArgsConstructor
public class AvaliacaoFuncionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "avaliacao_id", nullable = false)
    private Avaliacao avaliacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pergunta_id", nullable = false)
    private Pergunta pergunta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resposta_id")
    private Resposta resposta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resposta_gestor_id")
    private Resposta respostaGestor;
}
