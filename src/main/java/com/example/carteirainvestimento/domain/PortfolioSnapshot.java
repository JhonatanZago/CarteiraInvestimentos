package com.example.carteirainvestimento.domain;

import com.example.carteirainvestimento.enums.OrigemSnapshotCarteira;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.dto.financeiro.DisponibilidadeMonetaria;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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
    @Enumerated(EnumType.STRING) @Column(name = "moeda_base", length = 3) private Moeda moedaBase;
    @Column(name = "total_investido_base", precision = 19, scale = 8) private BigDecimal totalInvestidoBase;
    @Column(name = "patrimonio_atual_base", precision = 19, scale = 8) private BigDecimal patrimonioAtualBase;
    @Column(name = "resultado_base", precision = 19, scale = 8) private BigDecimal resultadoBase;
    @Column(name = "rentabilidade_base", precision = 19, scale = 8) private BigDecimal rentabilidadeBase;
    @Enumerated(EnumType.STRING) @Column(name = "disponibilidade_monetaria", length = 16) private DisponibilidadeMonetaria disponibilidadeMonetaria;
    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PortfolioSnapshotCambio> cambios = new ArrayList<>();

    public void substituirCambios(List<PortfolioSnapshotCambio> novosCambios) {
        cambios.clear();
        novosCambios.forEach(cambio -> {
            cambio.setSnapshot(this);
            cambios.add(cambio);
        });
    }
}
