package com.example.carteirainvestimento.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CalculadoraMovimentacaoTest {
    @Test
    void calculatesOriginalAndConvertedValuesWithDecimalPrecision() {
        BigDecimal original = CalculadoraMovimentacao.valorOriginal(new BigDecimal("2.5"), new BigDecimal("10.20"));
        assertThat(original).isEqualByComparingTo("25.50");
        assertThat(CalculadoraMovimentacao.valorEmBrl(original, new BigDecimal("5.25")))
                .isEqualByComparingTo("133.875");
    }

    @Test
    void calculatesRealizedResultAndCosts() {
        assertThat(CalculadoraMovimentacao.resultadoVenda(new BigDecimal("120"), new BigDecimal("100"),
                new BigDecimal("2.50"))).isEqualByComparingTo("17.50");
    }
}
