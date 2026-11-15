package com.example.carteirainvestimento.service;

import com.example.carteirainvestimento.dto.financeiro.ContextoMonetario;
import com.example.carteirainvestimento.dto.financeiro.DisponibilidadeMonetaria;
import com.example.carteirainvestimento.dto.insight.DisponibilidadeInsight;
import com.example.carteirainvestimento.dto.insight.IndicadorMercadoResponse;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.integration.adapter.MarketIndicatorAdapter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExchangeRateService {
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final String UNAVAILABLE_SOURCE = "FX_PROVIDER_UNAVAILABLE";
    private static final String INVALID_CURRENCY_SOURCE = "FX_INVALID_CURRENCY";
    private static final String UNSUPPORTED_PAIR_SOURCE = "FX_PAIR_UNSUPPORTED";
    private static final String IDENTITY_SOURCE = "IDENTITY";
    private static final BigDecimal ONE = BigDecimal.ONE;

    private final List<MarketIndicatorAdapter> adapters;
    private final Clock clock;
    private volatile ContextoMonetario cachedUsdToBrl;
    private volatile Instant cachedAt;

    @Autowired
    public ExchangeRateService(List<MarketIndicatorAdapter> adapters) {
        this(adapters, Clock.systemUTC());
    }

    ExchangeRateService(List<MarketIndicatorAdapter> adapters, Clock clock) {
        this.adapters = List.copyOf(adapters);
        this.clock = clock;
    }

    public synchronized ContextoMonetario usdToBrlContext() {
        Instant now = clock.instant();
        if (cachedUsdToBrl != null && cachedAt != null) {
            Duration age = Duration.between(cachedAt, now);
            if (!age.isNegative() && age.compareTo(CACHE_TTL) < 0) {
                return cachedUsdToBrl;
            }
        }

        for (MarketIndicatorAdapter adapter : adapters) {
            try {
                for (IndicadorMercadoResponse indicator : adapter.buscarIndicadores()) {
                    if (!isValidUsdBrlIndicator(indicator, adapter)) {
                        continue;
                    }
                    ContextoMonetario context = ContextoMonetario.available(
                            Moeda.USD, Moeda.BRL, ONE, indicator.valor(), indicator.valor(),
                            indicator.referenciaEm(), adapter.fonte());
                    cachedUsdToBrl = context;
                    cachedAt = now;
                    return context;
                }
            } catch (RuntimeException ignored) {
                // A próxima fonte pode atender a consulta sem expor detalhes do provedor.
            }
        }

        cachedUsdToBrl = null;
        cachedAt = now;
        return ContextoMonetario.unavailable(Moeda.USD, Moeda.BRL, ONE, UNAVAILABLE_SOURCE);
    }

    public ContextoMonetario obterTaxa(Moeda origem, Moeda destino) {
        if (origem == null || destino == null) {
            return ContextoMonetario.unavailable(origem, destino, null, INVALID_CURRENCY_SOURCE);
        }
        if (origem == destino) {
            OffsetDateTime now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
            return ContextoMonetario.available(origem, destino, ONE, ONE, ONE, now, IDENTITY_SOURCE);
        }
        if (!((origem == Moeda.USD && destino == Moeda.BRL)
                || (origem == Moeda.BRL && destino == Moeda.USD))) {
            return ContextoMonetario.unavailable(origem, destino, null, UNSUPPORTED_PAIR_SOURCE);
        }

        ContextoMonetario usdToBrl = usdToBrlContext();
        if (usdToBrl.disponibilidade() == DisponibilidadeMonetaria.UNAVAILABLE) {
            return ContextoMonetario.unavailable(origem, destino, null, usdToBrl.fonteCambio());
        }
        if (origem == Moeda.USD && destino == Moeda.BRL) {
            return usdToBrl;
        }
        if (origem == Moeda.BRL && destino == Moeda.USD) {
            BigDecimal inverse = ONE.divide(usdToBrl.taxaCambio(), 16, RoundingMode.HALF_EVEN);
            return ContextoMonetario.available(Moeda.BRL, Moeda.USD, ONE, inverse, inverse,
                    usdToBrl.cambioEfetivoEm(), usdToBrl.fonteCambio());
        }
        return ContextoMonetario.unavailable(origem, destino, null, UNSUPPORTED_PAIR_SOURCE);
    }

    /** Compatibilidade com consumidores legados que ainda precisam apenas da taxa USD/BRL. */
    public BigDecimal usdToBrl() {
        return usdToBrlContext().valorConvertidoBase();
    }

    private static boolean isValidUsdBrlIndicator(IndicadorMercadoResponse indicator,
            MarketIndicatorAdapter adapter) {
        if (indicator == null || !"USD".equalsIgnoreCase(indicator.codigo())
                || indicator.disponibilidade() != DisponibilidadeInsight.AVAILABLE
                || indicator.valor() == null || indicator.valor().signum() <= 0
                || indicator.referenciaEm() == null
                || indicator.referenciaEm().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            return false;
        }
        return adapter.fonte() != null && !adapter.fonte().isBlank();
    }
}
