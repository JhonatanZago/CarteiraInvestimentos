package com.example.carteirainvestimento.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record AdminPasswordRequest(
        @NotBlank String senha,
        @NotBlank String confirmacaoSenha) {
}
