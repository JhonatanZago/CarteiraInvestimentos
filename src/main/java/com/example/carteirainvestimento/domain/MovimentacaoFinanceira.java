package com.example.carteirainvestimento.domain;

import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.enums.TipoMovimentacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro imutável (por operação) dos valores originais e cambiais.
 * A entidade AtivoCarteira continua sendo a projeção compatível usada pelos
 * endpoints atuais; esta tabela permite evoluir para custo histórico sem
 * perder a moeda ou o câmbio da operação.
 */
@Entity
@Table(name = "movimentacao_financeira", indexes = {
        @Index(name = "idx_movimentacao_carteira_data", columnList = "carteira_id,data_operacao"),
        @Index(name = "idx_movimentacao_acao", columnList = "acao_id")
})
@Getter
@Setter
@NoArgsConstructor
public class MovimentacaoFinanceira {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TipoMovimentacao tipo;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 19, scale = 8)
    private BigDecimal precoUnitario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Moeda moeda;

    @Column(name = "cambio_para_brl", precision = 19, scale = 8)
    private BigDecimal cambioParaBrl;

    @Column(name = "cambio_em")
    private OffsetDateTime cambioEm;

    @Column(name = "cambio_fonte", length = 80)
    private String cambioFonte;

    @Column(name = "valor_original", nullable = false, precision = 19, scale = 8)
    private BigDecimal valorOriginal;

    @Column(name = "valor_brl", precision = 19, scale = 8)
    private BigDecimal valorBrl;

    @Column(name = "custos_brl", precision = 19, scale = 8)
    private BigDecimal custosBrl;

    @Column(name = "data_operacao", nullable = false)
    private LocalDate dataOperacao;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    void initializeCreatedAt() {
        if (criadoEm == null) {
            criadoEm = OffsetDateTime.now();
        }
    }
}
