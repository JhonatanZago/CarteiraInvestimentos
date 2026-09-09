package com.example.carteirainvestimento.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ativo_carteira", uniqueConstraints = @UniqueConstraint(
        name = "uk_ativo_carteira_carteira_acao_corretora",
        columnNames = {"carteira_id", "acao_id", "corretora_id"}))
@Getter
@Setter
@NoArgsConstructor
public class AtivoCarteira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carteira_id", nullable = false)
    private Carteira carteira;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acao_id", nullable = false)
    private Acao acao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "corretora_id", nullable = false)
    private Corretora corretora;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantidade;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal precoMedio;

    @Column(nullable = false)
    private LocalDate dataPrimeiraCompra;

    /**
     * Posições legadas não possuem o câmbio da compra comprovado. Nelas,
     * qualquer consolidação em BRL deve ser identificada como estimada.
     */
    @Column(name = "cambio_historico_comprovado")
    private Boolean cambioHistoricoComprovado = Boolean.FALSE;

    public boolean isConversaoEstimada() {
        return acao != null && acao.getMoeda() != com.example.carteirainvestimento.enums.Moeda.BRL
                && !Boolean.TRUE.equals(cambioHistoricoComprovado);
    }
}
