package com.example.carteirainvestimento.dto.acao;

import com.example.carteirainvestimento.enums.CountryCode;
import com.example.carteirainvestimento.enums.Mercado;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record AcaoCreateRequest(@NotBlank String ticker, @NotNull CountryCode countryCode) {
    /** Compatibilidade para testes internos legados; o contrato HTTP é countryCode. */
    public AcaoCreateRequest(String ticker, Mercado mercado, String countryCode) {
        this(ticker, countryCode == null || countryCode.isBlank() ? null : CountryCode.valueOf(countryCode.trim().toUpperCase()));
    }
}
