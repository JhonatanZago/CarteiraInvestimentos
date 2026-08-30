package com.example.carteirainvestimento.integration.dto;

import java.time.OffsetDateTime;

public record InstituicaoFinanceiraConsulta(
        boolean autorizada,
        String fonte,
        OffsetDateTime dataHoraValidacao) {
}
