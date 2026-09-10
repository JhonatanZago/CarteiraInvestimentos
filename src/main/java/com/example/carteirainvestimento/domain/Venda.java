package com.example.carteirainvestimento.domain;

import com.example.carteirainvestimento.enums.Moeda;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "venda")
@Getter @Setter @NoArgsConstructor
public class Venda {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "carteira_id", nullable = false) private Carteira carteira;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "acao_id", nullable = false) private Acao acao;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "corretora_id", nullable = false) private Corretora corretora;
    @Column(name = "posicao_id", nullable = false) private Long posicaoId;
    @Column(nullable = false, precision = 19, scale = 6) private BigDecimal quantidade;
    @Column(name = "preco_medio", nullable = false, precision = 19, scale = 8) private BigDecimal precoMedio;
    @Column(name = "preco_venda", nullable = false, precision = 19, scale = 8) private BigDecimal precoVenda;
    @Column(nullable = false, precision = 19, scale = 8) private BigDecimal taxas;
    @Column(name = "valor_bruto", nullable = false, precision = 19, scale = 8) private BigDecimal valorBruto;
    @Column(name = "custo_posicao", nullable = false, precision = 19, scale = 8) private BigDecimal custoPosicao;
    @Column(name = "resultado_realizado", nullable = false, precision = 19, scale = 8) private BigDecimal resultadoRealizado;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 3) private Moeda moeda;
    @Column(name = "data_venda", nullable = false) private LocalDate dataVenda;
    @Column(name = "criado_em", nullable = false) private OffsetDateTime criadoEm;
    @PrePersist void created() { if (criadoEm == null) criadoEm = OffsetDateTime.now(); }
}
