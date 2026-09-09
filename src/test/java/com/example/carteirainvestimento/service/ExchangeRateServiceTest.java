package com.example.carteirainvestimento.service;

import com.example.carteirainvestimento.dto.insight.DisponibilidadeInsight;
import com.example.carteirainvestimento.dto.insight.IndicadorMercadoResponse;
import com.example.carteirainvestimento.integration.adapter.MarketIndicatorAdapter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExchangeRateServiceTest {
    @Test
    void selectsFirstPositiveAvailableSourceAndCachesIt() {
        var unavailable = (MarketIndicatorAdapter) () -> List.of(new IndicadorMercadoResponse("USD", "Dólar", null, null, null, DisponibilidadeInsight.UNAVAILABLE));
        var fallback = (MarketIndicatorAdapter) () -> List.of(new IndicadorMercadoResponse("USD", "Dólar", new BigDecimal("5.25"), null, null, DisponibilidadeInsight.AVAILABLE));
        var service = new ExchangeRateService(List.of(unavailable, fallback));
        assertThat(service.usdToBrl()).isEqualByComparingTo("5.25");
        assertThat(service.usdToBrl()).isEqualByComparingTo("5.25");
    }

    @Test
    void ignoresZeroAndUnavailableRates() {
        var zero = (MarketIndicatorAdapter) () -> List.of(new IndicadorMercadoResponse("USD", "Dólar", BigDecimal.ZERO, null, null, DisponibilidadeInsight.AVAILABLE));
        var service = new ExchangeRateService(List.of(zero));
        assertThat(service.usdToBrl()).isNull();
    }
}
