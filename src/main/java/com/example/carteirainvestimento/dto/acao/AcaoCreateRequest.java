package com.example.carteirainvestimento.dto.acao;

import com.example.carteirainvestimento.enums.Mercado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AcaoCreateRequest(@NotBlank String ticker, @NotNull Mercado mercado) {
}
