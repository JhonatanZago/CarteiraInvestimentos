package com.example.carteirainvestimento.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CalculadoraFinanceiraTest {

    @Test
    void calculaValoresUsandoPrecoMedioECotacaoAtual() {
        PosicaoFinanceira resultado = CalculadoraFinanceira.calcular(new BigDecimal("10"), new BigDecimal("45"), new BigDecimal("48.20"));

        assertThat(resultado.valorInvestido()).isEqualByComparingTo("450.00");
        assertThat(resultado.valorAtual()).isEqualByComparingTo("482.00");
        assertThat(resultado.resultado()).isEqualByComparingTo("32.00");
        assertThat(resultado.rentabilidadePercentual()).isEqualByComparingTo("7.1111");
    }

    @Test
    void calculaResultadoNegativoQuandoCotacaoFicaAbaixoDoPrecoMedio() {
        PosicaoFinanceira resultado = CalculadoraFinanceira.calcular(new BigDecimal("2"), new BigDecimal("30"), new BigDecimal("25"));

        assertThat(resultado.resultado()).isEqualByComparingTo("-10.00");
        assertThat(resultado.rentabilidadePercentual()).isEqualByComparingTo("-16.6667");
    }

    @Test
    void mantemValorAtualIndisponivelSemUsarPrecoMedioComoFallback() {
        PosicaoFinanceira resultado = CalculadoraFinanceira.calcular(new BigDecimal("2"), new BigDecimal("30"), null);

        assertThat(resultado.valorInvestido()).isEqualByComparingTo("60.00");
        assertThat(resultado.valorAtual()).isNull();
        assertThat(resultado.resultado()).isNull();
        assertThat(resultado.rentabilidadePercentual()).isNull();
    }

    @Test
    void novaCotacaoAlteraValorAtualSemAlterarPrecoMedioInformado() {
        BigDecimal precoMedio = new BigDecimal("25");
        PosicaoFinanceira antes = CalculadoraFinanceira.calcular(new BigDecimal("2"), precoMedio, new BigDecimal("30"));
        PosicaoFinanceira depois = CalculadoraFinanceira.calcular(new BigDecimal("2"), precoMedio, new BigDecimal("35"));

        assertThat(precoMedio).isEqualByComparingTo("25");
        assertThat(antes.valorAtual()).isEqualByComparingTo("60.00");
        assertThat(depois.valorAtual()).isEqualByComparingTo("70.00");
        assertThat(depois.resultado()).isEqualByComparingTo("20.00");
    }
}
