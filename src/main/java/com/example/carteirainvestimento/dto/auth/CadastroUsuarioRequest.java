package com.example.carteirainvestimento.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CadastroUsuarioRequest(@NotBlank String nome, @NotBlank @Email String email,
                                     @NotBlank String senha, @NotBlank String confirmacaoSenha) { }
