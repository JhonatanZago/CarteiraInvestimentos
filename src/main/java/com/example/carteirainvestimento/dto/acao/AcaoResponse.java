package com.example.carteirainvestimento.dto.acao;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;

public record AcaoResponse(Long id, String ticker, String nomeEmpresa, Mercado mercado, Moeda moeda,
                           BigDecimal cotacaoAtual, OffsetDateTime dataHoraCotacao, String logoUrl,
                           String listingCountryCode, String exchange, String exchangeMic, String dataSource) {
    public AcaoResponse(Long id, String ticker, String nomeEmpresa, Mercado mercado, Moeda moeda,
                        BigDecimal cotacaoAtual, OffsetDateTime dataHoraCotacao, String logoUrl) {
        this(id, ticker, nomeEmpresa, mercado, moeda, cotacaoAtual, dataHoraCotacao, logoUrl, null, null, null, null);
    }
}
