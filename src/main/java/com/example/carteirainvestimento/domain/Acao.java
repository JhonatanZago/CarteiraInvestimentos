package com.example.carteirainvestimento.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "acao", uniqueConstraints = @UniqueConstraint(name = "uk_acao_ticker", columnNames = "ticker"))
@Getter
@Setter
@NoArgsConstructor
public class Acao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(nullable = false)
    private String nomeEmpresa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Mercado mercado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Moeda moeda;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal cotacaoAtual;

    @Column(nullable = false)
    private OffsetDateTime dataHoraCotacao;

    @Column(length = 2048)
    private String logoUrl;

    @Column(length = 2)
    private String listingCountryCode;
    @Column(length = 80)
    private String exchange;
    @Column(length = 12)
    private String exchangeMic;
    @Column(length = 40)
    private String dataSource;
}
