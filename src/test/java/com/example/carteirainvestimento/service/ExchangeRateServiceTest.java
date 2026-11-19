package com.example.carteirainvestimento.service;

import com.example.carteirainvestimento.dto.financeiro.DisponibilidadeMonetaria;
import com.example.carteirainvestimento.dto.insight.DisponibilidadeInsight;
import com.example.carteirainvestimento.dto.insight.IndicadorMercadoResponse;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.integration.adapter.MarketIndicatorAdapter;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExchangeRateServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-15T12:00:00Z");
    private static final OffsetDateTime REFERENCE = OffsetDateTime.parse("2026-11-15T11:55:00Z");

    @Test
    void returnsValidatedRateProvenanceAndCachesIt() {
        AtomicInteger calls = new AtomicInteger();
        MarketIndicatorAdapter source = () -> {
            calls.incrementAndGet();
            return List.of(indicator(new BigDecimal("5.25"), REFERENCE, DisponibilidadeInsight.AVAILABLE));
        };
        ExchangeRateService service = new ExchangeRateService(List.of(source), fixedClock(NOW));

        var context = service.usdToBrlContext();

        assertThat(context.moedaOriginal()).isEqualTo(Moeda.USD);
        assertThat(context.moedaBase()).isEqualTo(Moeda.BRL);
        assertThat(context.valorOriginal()).isEqualByComparingTo("1");
        assertThat(context.valorConvertidoBase()).isEqualByComparingTo("5.25");
        assertThat(context.taxaCambio()).isEqualByComparingTo("5.25");
        assertThat(context.cambioEfetivoEm()).isEqualTo(REFERENCE);
        assertThat(context.fonteCambio()).isNotBlank();
        assertThat(context.disponibilidade()).isEqualTo(DisponibilidadeMonetaria.AVAILABLE);
        assertThat(service.usdToBrl()).isEqualByComparingTo("5.25");
        assertThat(calls).hasValue(1);
    }

    @Test
    void rejectsZeroNegativeMissingDateAndMissingSourceRates() {
        MarketIndicatorAdapter zero = () -> List.of(indicator(BigDecimal.ZERO, REFERENCE, DisponibilidadeInsight.AVAILABLE));
        MarketIndicatorAdapter negative = () -> List.of(indicator(new BigDecimal("-1"), REFERENCE, DisponibilidadeInsight.AVAILABLE));
        MarketIndicatorAdapter missingDate = () -> List.of(indicator(new BigDecimal("5"), null, DisponibilidadeInsight.AVAILABLE));
        MarketIndicatorAdapter missingSource = new MarketIndicatorAdapter() {
            @Override public List<IndicadorMercadoResponse> buscarIndicadores() {
                return List.of(indicator(new BigDecimal("5"), REFERENCE, DisponibilidadeInsight.AVAILABLE));
            }
            @Override public String fonte() { return " "; }
        };
        var service = new ExchangeRateService(List.of(zero, negative, missingDate, missingSource), fixedClock(NOW));

        assertThat(service.usdToBrlContext().disponibilidade()).isEqualTo(DisponibilidadeMonetaria.UNAVAILABLE);
        assertThat(service.usdToBrl()).isNull();
    }

    @Test
    void rejectsUnavailableAndFutureProviderDataWithoutInventingRate() {
        MarketIndicatorAdapter unavailable = () -> List.of(indicator(new BigDecimal("5"), REFERENCE, DisponibilidadeInsight.UNAVAILABLE));
        MarketIndicatorAdapter future = () -> List.of(indicator(new BigDecimal("6"), REFERENCE.plusDays(1), DisponibilidadeInsight.AVAILABLE));
        var service = new ExchangeRateService(List.of(unavailable, future), fixedClock(NOW));

        var context = service.usdToBrlContext();

        assertThat(context.disponibilidade()).isEqualTo(DisponibilidadeMonetaria.UNAVAILABLE);
        assertThat(context.valorConvertidoBase()).isNull();
        assertThat(context.taxaCambio()).isNull();
        assertThat(context.fonteCambio()).isEqualTo("FX_PROVIDER_UNAVAILABLE");
    }

    @Test
    void supportsBothDirectionsUsingTheSameEffectiveRate() {
        MarketIndicatorAdapter source = () -> List.of(indicator(new BigDecimal("5.00"), REFERENCE, DisponibilidadeInsight.AVAILABLE));
        var service = new ExchangeRateService(List.of(source), fixedClock(NOW));

        var usdToBrl = service.obterTaxa(Moeda.USD, Moeda.BRL);
        var brlToUsd = service.obterTaxa(Moeda.BRL, Moeda.USD);

        assertThat(usdToBrl.taxaCambio()).isEqualByComparingTo("5.00");
        assertThat(brlToUsd.taxaCambio()).isEqualByComparingTo("0.2");
        assertThat(brlToUsd.cambioEfetivoEm()).isEqualTo(REFERENCE);
        assertThat(brlToUsd.fonteCambio()).isEqualTo(usdToBrl.fonteCambio());
    }

    @Test
    void marksUnsupportedPairsUnavailableAndIdentityAsAvailable() {
        var service = new ExchangeRateService(List.of(), fixedClock(NOW));

        var identity = service.obterTaxa(Moeda.BRL, Moeda.BRL);
        var unsupported = service.obterTaxa(Moeda.EUR, Moeda.USD);

        assertThat(identity.disponibilidade()).isEqualTo(DisponibilidadeMonetaria.AVAILABLE);
        assertThat(identity.taxaCambio()).isEqualByComparingTo("1");
        assertThat(unsupported.disponibilidade()).isEqualTo(DisponibilidadeMonetaria.UNAVAILABLE);
        assertThat(unsupported.fonteCambio()).isEqualTo("FX_PAIR_UNSUPPORTED");
    }

    @Test
    void refreshesAfterCacheTtl() {
        MutableClock clock = new MutableClock(NOW);
        AtomicInteger calls = new AtomicInteger();
        MarketIndicatorAdapter source = () -> {
            calls.incrementAndGet();
            return List.of(indicator(new BigDecimal("5.25"), REFERENCE, DisponibilidadeInsight.AVAILABLE));
        };
        var service = new ExchangeRateService(List.of(source), clock);

        service.usdToBrlContext();
        clock.advance(Duration.ofMinutes(5));
        service.usdToBrlContext();

        assertThat(calls).hasValue(2);
    }

    @Test
    void handlesNullCurrenciesAndProviderExceptionsWithoutInventingRates() {
        var service = new ExchangeRateService(List.of(() -> {
            throw new IllegalStateException("provider down");
        }), fixedClock(NOW));

        assertThat(service.obterTaxa(null, Moeda.BRL).disponibilidade())
                .isEqualTo(DisponibilidadeMonetaria.UNAVAILABLE);
        assertThat(service.obterTaxa(Moeda.USD, null).fonteCambio())
                .isEqualTo("FX_INVALID_CURRENCY");
        assertThat(service.usdToBrlContext().disponibilidade())
                .isEqualTo(DisponibilidadeMonetaria.UNAVAILABLE);
    }

    @Test
    void skipsWrongIndicatorCodeAndBlankSource() {
        MarketIndicatorAdapter wrongCode = new MarketIndicatorAdapter() {
            @Override public List<IndicadorMercadoResponse> buscarIndicadores() {
                return List.of(new IndicadorMercadoResponse("CDI", "CDI", new BigDecimal("5"),
                        null, REFERENCE, DisponibilidadeInsight.AVAILABLE));
            }
            @Override public String fonte() { return "SOURCE"; }
        };
        MarketIndicatorAdapter blankSource = new MarketIndicatorAdapter() {
            @Override public List<IndicadorMercadoResponse> buscarIndicadores() {
                return List.of(indicator(new BigDecimal("5"), REFERENCE, DisponibilidadeInsight.AVAILABLE));
            }
            @Override public String fonte() { return ""; }
        };

        var service = new ExchangeRateService(List.of(wrongCode, blankSource), fixedClock(NOW));

        assertThat(service.usdToBrlContext().disponibilidade())
                .isEqualTo(DisponibilidadeMonetaria.UNAVAILABLE);
    }

    private static IndicadorMercadoResponse indicator(BigDecimal value, OffsetDateTime reference,
            DisponibilidadeInsight availability) {
        return new IndicadorMercadoResponse("USD", "Dólar", value, null, reference, availability);
    }

    private static Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) { this.instant = instant; }
        private void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
