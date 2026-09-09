package com.example.carteirainvestimento.dto.acao;

import com.example.carteirainvestimento.enums.Mercado;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record AcaoCreateRequest(@NotBlank String ticker, @NotNull Mercado mercado,
        @NotBlank String selectedCountryCode) {
    /** Compatibility constructor for internal callers; HTTP requests must provide the country explicitly. */
    public AcaoCreateRequest(String ticker, Mercado mercado) {
        this(ticker, mercado, mercado == Mercado.BRASIL ? "BR" : "US");
    }
}
