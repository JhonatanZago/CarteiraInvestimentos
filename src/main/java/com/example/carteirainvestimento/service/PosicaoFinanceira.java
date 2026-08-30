package com.example.carteirainvestimento.service;

import java.math.BigDecimal;

public record PosicaoFinanceira(
        BigDecimal valorInvestido,
        BigDecimal valorAtual,
        BigDecimal resultado,
        BigDecimal rentabilidadePercentual) {
}
