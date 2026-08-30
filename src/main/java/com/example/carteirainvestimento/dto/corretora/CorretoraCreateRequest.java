package com.example.carteirainvestimento.dto.corretora;

import jakarta.validation.constraints.NotBlank;

public record CorretoraCreateRequest(
        @NotBlank String cnpj,
        @NotBlank String cep,
        @NotBlank String numero,
        String complemento) {
}
