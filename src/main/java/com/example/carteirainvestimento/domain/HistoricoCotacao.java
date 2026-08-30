package com.example.carteirainvestimento.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.example.carteirainvestimento.enums.FonteCotacao;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "historico_cotacao", uniqueConstraints = @UniqueConstraint(
        name = "uk_historico_cotacao_acao_data_hora",
        columnNames = {"acao_id", "data_hora_cotacao"}))
@Getter
@Setter
@NoArgsConstructor
public class HistoricoCotacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acao_id", nullable = false)
    private Acao acao;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal valor;

    @Column(nullable = false)
    private OffsetDateTime dataHoraCotacao;

    @Column(nullable = false)
    private OffsetDateTime dataHoraRegistro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FonteCotacao fonte;
}
