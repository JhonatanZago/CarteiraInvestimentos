package com.example.carteirainvestimento.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Operações decimais puras usadas para preservar precisão financeira. */
public final class CalculadoraMovimentacao {
    private static final int SCALE = 8;

    private CalculadoraMovimentacao() {
    }

    public static BigDecimal valorOriginal(BigDecimal quantidade, BigDecimal precoUnitario) {
        return quantidade.multiply(precoUnitario).setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal valorEmBrl(BigDecimal valorOriginal, BigDecimal cambioParaBrl) {
        if (cambioParaBrl == null || cambioParaBrl.signum() <= 0) {
            return null;
        }
        return valorOriginal.multiply(cambioParaBrl).setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal resultadoVenda(BigDecimal valorVenda, BigDecimal custo,
            BigDecimal custosBrl) {
        return valorVenda.subtract(custo).subtract(custosBrl == null ? BigDecimal.ZERO : custosBrl)
                .setScale(SCALE, RoundingMode.HALF_EVEN);
    }
}
