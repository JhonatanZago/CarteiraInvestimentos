package com.example.carteirainvestimento.integration.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.example.carteirainvestimento.enums.FonteCotacao;
import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;

public record CotacaoConsulta(
        String ticker,
        String nomeEmpresa,
        Mercado mercado,
        Moeda moeda,
        BigDecimal valor,
        OffsetDateTime dataHoraCotacao,
        FonteCotacao fonte) {
}
