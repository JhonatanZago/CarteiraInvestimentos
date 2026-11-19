package com.example.carteirainvestimento.dto.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.carteirainvestimento.enums.Moeda;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class ContextoMonetarioTest {
    private static final OffsetDateTime CAMBIO_EM = OffsetDateTime.parse("2026-11-14T12:00:00Z");

    @Test
    void representsAvailableBrlWithoutChangingTheOriginalValue() {
        var contexto = ContextoMonetario.available(Moeda.BRL, Moeda.BRL,
                new BigDecimal("123.456789"), new BigDecimal("123.456789"),
                BigDecimal.ONE, CAMBIO_EM, "IDENTITY");

        assertThat(contexto.disponibilidade()).isEqualTo(DisponibilidadeMonetaria.AVAILABLE);
        assertThat(contexto.valorOriginal()).isEqualByComparingTo("123.456789");
        assertThat(contexto.valorConvertidoBase()).isEqualByComparingTo("123.456789");
    }

    @Test
    void representsAvailableUsdWithConversionProvenance() {
        var contexto = ContextoMonetario.available(Moeda.USD, Moeda.BRL,
                new BigDecimal("10.12345678"), new BigDecimal("52.648"),
                new BigDecimal("5.20"), CAMBIO_EM, "BANCO_CENTRAL");

        assertThat(contexto.moedaOriginal()).isEqualTo(Moeda.USD);
        assertThat(contexto.moedaBase()).isEqualTo(Moeda.BRL);
        assertThat(contexto.taxaCambio()).isEqualByComparingTo("5.20");
        assertThat(contexto.fonteCambio()).isEqualTo("BANCO_CENTRAL");
    }

    @Test
    void allowsUnavailableLegacyValueWithoutInventingCurrencyOrRate() {
        var contexto = ContextoMonetario.unavailable(null, Moeda.BRL,
                new BigDecimal("10.00"), "LEGACY_REVIEW_REQUIRED");

        assertThat(contexto.disponibilidade()).isEqualTo(DisponibilidadeMonetaria.UNAVAILABLE);
        assertThat(contexto.moedaOriginal()).isNull();
        assertThat(contexto.taxaCambio()).isNull();
        assertThat(contexto.valorConvertidoBase()).isNull();
    }

    @Test
    void rejectsInvalidExchangeRate() {
        assertThatThrownBy(() -> ContextoMonetario.available(Moeda.USD, Moeda.BRL,
                new BigDecimal("10"), new BigDecimal("50"), BigDecimal.ZERO,
                CAMBIO_EM, "BANCO_CENTRAL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positiva");
    }

    @Test
    void requiresIdentifiableSourceForEstimatedValue() {
        assertThatThrownBy(() -> ContextoMonetario.estimated(Moeda.USD, Moeda.BRL,
                new BigDecimal("10"), new BigDecimal("50"), new BigDecimal("5"),
                CAMBIO_EM, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fonte");
    }

    @Test
    void preservesEstimatedStateWithoutCallingItAvailable() {
        var contexto = ContextoMonetario.estimated(Moeda.USD, Moeda.BRL,
                new BigDecimal("10"), new BigDecimal("50"), new BigDecimal("5"),
                CAMBIO_EM, "HISTORICAL_ESTIMATE");

        assertThat(contexto.disponibilidade()).isEqualTo(DisponibilidadeMonetaria.ESTIMATED);
        assertThat(contexto.valorOriginal()).isEqualByComparingTo("10");
    }

    @Test
    void serializesStateAndBigDecimalWithoutUsingFloatingPoint() throws Exception {
        var contexto = ContextoMonetario.available(Moeda.USD, Moeda.BRL,
                new BigDecimal("0.10"), new BigDecimal("0.52"), new BigDecimal("5.20"),
                CAMBIO_EM, "BANCO_CENTRAL");
        var json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(contexto);

        assertThat(json).contains("\"moedaOriginal\":\"USD\"");
        assertThat(json).contains("\"disponibilidade\":\"AVAILABLE\"");
        assertThat(json).contains("\"valorOriginal\":0.10");
        assertThat(json).contains("\"taxaCambio\":5.20");
    }

    @Test
    void rejectsIncompleteAvailableAndUnavailableContexts() {
        assertThatThrownBy(() -> ContextoMonetario.available(null, Moeda.BRL,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, CAMBIO_EM, "IDENTITY"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ContextoMonetario.available(Moeda.USD, Moeda.BRL,
                BigDecimal.ONE, null, BigDecimal.ONE, CAMBIO_EM, "FX"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ContextoMonetario(Moeda.USD, Moeda.BRL, BigDecimal.ONE,
                BigDecimal.ONE, null, null, "FX", DisponibilidadeMonetaria.UNAVAILABLE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsSameCurrencyReconversionAndMissingCrossCurrencyProvenance() {
        assertThatThrownBy(() -> ContextoMonetario.available(Moeda.BRL, Moeda.BRL,
                BigDecimal.TEN, new BigDecimal("11"), BigDecimal.ONE, CAMBIO_EM, "FX"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ContextoMonetario.available(Moeda.USD, Moeda.BRL,
                BigDecimal.TEN, new BigDecimal("50"), null, CAMBIO_EM, "FX"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ContextoMonetario.available(Moeda.USD, Moeda.BRL,
                BigDecimal.TEN, new BigDecimal("50"), BigDecimal.ONE, null, "FX"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeExchangeRateAndUnavailableConvertedValue() {
        assertThatThrownBy(() -> new ContextoMonetario(Moeda.USD, Moeda.BRL, BigDecimal.ONE,
                null, new BigDecimal("-1"), CAMBIO_EM, "FX", DisponibilidadeMonetaria.UNAVAILABLE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
