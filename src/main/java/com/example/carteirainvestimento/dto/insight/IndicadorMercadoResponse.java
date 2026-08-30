package com.example.carteirainvestimento.dto.insight;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record IndicadorMercadoResponse(String codigo, String descricao, BigDecimal valor,
        BigDecimal variacaoPercentual, OffsetDateTime referenciaEm, DisponibilidadeInsight disponibilidade) { }
