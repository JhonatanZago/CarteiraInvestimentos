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
        String logoUrl,
        FonteCotacao fonte,
        String listingCountryCode,
        String exchange,
        String exchangeMic) {
    public CotacaoConsulta(String ticker, String nomeEmpresa, Mercado mercado, Moeda moeda,
                           BigDecimal valor, OffsetDateTime dataHoraCotacao, String logoUrl, FonteCotacao fonte) {
        this(ticker, nomeEmpresa, mercado, moeda, valor, dataHoraCotacao, logoUrl, fonte, null, null, null);
    }
}
