package com.example.carteirainvestimento.dto.insight;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PontoEvolucaoCarteiraResponse(OffsetDateTime referenciaEm, BigDecimal valorInvestido,
        BigDecimal valorAtual, DisponibilidadeInsight disponibilidade, OffsetDateTime timestamp,
        BigDecimal investedAmount, BigDecimal currentAmount, BigDecimal resultAmount,
        BigDecimal returnPercentage) {
    public PontoEvolucaoCarteiraResponse(OffsetDateTime referenciaEm, BigDecimal valorInvestido,
            BigDecimal valorAtual, DisponibilidadeInsight disponibilidade) {
        this(referenciaEm, valorInvestido, valorAtual, disponibilidade, referenciaEm, valorInvestido,
                valorAtual, valorAtual == null || valorInvestido == null ? null : valorAtual.subtract(valorInvestido),
                valorInvestido != null && valorInvestido.signum() > 0 && valorAtual != null
                        ? valorAtual.subtract(valorInvestido).multiply(BigDecimal.valueOf(100)).divide(valorInvestido, 6, java.math.RoundingMode.HALF_UP) : null);
    }
}
