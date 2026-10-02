package br.com.govalue.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Perfil perfil;

    @Column(name = "data_inicio_vigencia", nullable = false)
    private LocalDate dataInicioVigencia;

    @Column(name = "data_fim_vigencia")
    private LocalDate dataFimVigencia;

    @Column(name = "verificacao_facial_habilitada", nullable = false)
    private boolean verificacaoFacialHabilitada;

    /** Array JSON de floats (embedding de 128 dimensoes) — opaco para o Java, so repassado ao face-service. */
    @Column(name = "verificacao_facial_embedding")
    private String verificacaoFacialEmbedding;

    @Column(name = "verificacao_facial_consentimento_em")
    private Instant verificacaoFacialConsentimentoEm;

    /** Mesma regra do login original: acesso so dentro da janela de vigencia. */
    public boolean vigenteEm(LocalDate data) {
        return !data.isBefore(dataInicioVigencia)
                && (dataFimVigencia == null || !data.isAfter(dataFimVigencia));
    }
}
