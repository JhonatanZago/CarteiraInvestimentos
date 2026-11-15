package com.example.carteirainvestimento.domain;

import com.example.carteirainvestimento.dto.financeiro.DisponibilidadeMonetaria;
import com.example.carteirainvestimento.enums.Moeda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "portfolio_snapshot_cambio", uniqueConstraints = @UniqueConstraint(
        name = "uk_snapshot_cambio_moeda", columnNames = {"snapshot_id", "moeda_original", "moeda_base"}))
@Getter
@Setter
@NoArgsConstructor
public class PortfolioSnapshotCambio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private PortfolioSnapshot snapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "moeda_original", nullable = false, length = 3)
    private Moeda moedaOriginal;

    @Enumerated(EnumType.STRING)
    @Column(name = "moeda_base", nullable = false, length = 3)
    private Moeda moedaBase;

    @Column(name = "taxa_cambio", precision = 19, scale = 8)
    private BigDecimal taxaCambio;

    @Column(name = "cambio_efetivo_em")
    private OffsetDateTime cambioEfetivoEm;

    @Column(name = "fonte_cambio", length = 80)
    private String fonteCambio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DisponibilidadeMonetaria disponibilidade;
}
