package com.example.carteirainvestimento.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RedefinirSenhaRequest(@NotBlank String token, @NotBlank String senha, @NotBlank String confirmacaoSenha) { }
