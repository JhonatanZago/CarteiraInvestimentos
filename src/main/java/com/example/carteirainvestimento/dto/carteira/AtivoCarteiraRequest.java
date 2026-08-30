package com.example.carteirainvestimento.dto.carteira;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

public record AtivoCarteiraRequest(
        @NotNull Long acaoId,
        @NotNull Long corretoraId,
        @NotNull @DecimalMin(value = "0.000001") BigDecimal quantidade,
        @NotNull @DecimalMin(value = "0.01") BigDecimal precoMedio,
        @NotNull @PastOrPresent LocalDate dataPrimeiraCompra) {
}
