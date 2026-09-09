package com.example.carteirainvestimento.service;

import com.example.carteirainvestimento.dto.insight.IndicadorMercadoResponse;
import com.example.carteirainvestimento.integration.adapter.MarketIndicatorAdapter;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ExchangeRateService {
    private final List<MarketIndicatorAdapter> adapters;
    private volatile BigDecimal cachedRate;
    private volatile Instant cachedAt;

    public ExchangeRateService(List<MarketIndicatorAdapter> adapters) { this.adapters = List.copyOf(adapters); }

    public synchronized BigDecimal usdToBrl() {
        if (cachedRate != null && cachedAt != null && Duration.between(cachedAt, Instant.now()).toMinutes() < 5) return cachedRate;
        for (MarketIndicatorAdapter adapter : adapters) {
            try {
                IndicadorMercadoResponse indicator = adapter.buscarIndicadores().stream()
                        .filter(item -> "USD".equalsIgnoreCase(item.codigo()) && item.valor() != null && item.valor().signum() > 0)
                        .findFirst().orElse(null);
                if (indicator != null) { cachedRate = indicator.valor(); cachedAt = Instant.now(); return cachedRate; }
            } catch (RuntimeException ignored) { }
        }
        return null;
    }
}
