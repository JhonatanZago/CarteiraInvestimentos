package com.example.carteirainvestimento.domain;

import com.example.carteirainvestimento.enums.OrigemSnapshotCarteira;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "portfolio_snapshot", indexes = @Index(name = "idx_portfolio_snapshot_carteira_data", columnList = "carteira_id,data_hora"))
@Getter @Setter @NoArgsConstructor
public class PortfolioSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    /** Sem cascade: histórico não pode ser removido por uma exclusão acidental. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "carteira_id", nullable = false) private Carteira carteira;
    @Column(name = "data_hora", nullable = false) private OffsetDateTime dataHora;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal totalInvestido;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal patrimonioAtual;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal resultado;
    @Column(precision = 19, scale = 6) private BigDecimal rentabilidade;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private OrigemSnapshotCarteira origem;
}
