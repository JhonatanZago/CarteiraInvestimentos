package com.example.carteirainvestimento.dto.insight;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ResumoProventosResponse(BigDecimal recebidosUltimosDozeMeses, BigDecimal proximosProventos,
        OffsetDateTime referenciaEm, DisponibilidadeInsight disponibilidade) { }
