package com.example.carteirainvestimento.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CalculadoraFinanceira {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO_MONETARIO = BigDecimal.ZERO.setScale(2);

    private CalculadoraFinanceira() {
    }

    public static PosicaoFinanceira calcular(BigDecimal quantidade, BigDecimal precoMedio, BigDecimal cotacaoAtual) {
        BigDecimal valorInvestido = multiplicar(quantidade, precoMedio);
        BigDecimal valorAtual = multiplicar(quantidade, cotacaoAtual);
        BigDecimal resultado = valorAtual.subtract(valorInvestido);
        BigDecimal rentabilidade = valorInvestido.signum() == 0
                ? BigDecimal.ZERO
                : resultado.multiply(CEM).divide(valorInvestido, 4, RoundingMode.HALF_UP);

        return new PosicaoFinanceira(monetario(valorInvestido), monetario(valorAtual), monetario(resultado), rentabilidade);
    }

    public static BigDecimal monetario(BigDecimal valor) {
        return valor.signum() == 0 ? ZERO_MONETARIO : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal multiplicar(BigDecimal primeiroFator, BigDecimal segundoFator) {
        if (primeiroFator == null || segundoFator == null) {
            return BigDecimal.ZERO;
        }
        return primeiroFator.multiply(segundoFator);
    }
}
