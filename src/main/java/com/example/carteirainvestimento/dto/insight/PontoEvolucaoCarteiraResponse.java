package com.example.carteirainvestimento.dto.insight;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PontoEvolucaoCarteiraResponse(OffsetDateTime referenciaEm, BigDecimal valorInvestido,
        BigDecimal valorAtual, DisponibilidadeInsight disponibilidade) { }
