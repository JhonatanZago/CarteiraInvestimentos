package com.example.carteirainvestimento.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SolicitarRecuperacaoSenhaRequest(@NotBlank @Email String email) { }
