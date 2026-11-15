package com.example.carteirainvestimento.dto.admin;

import com.example.carteirainvestimento.enums.PerfilUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminUpdateUserRequest(
        @NotBlank String nome,
        @NotBlank @Email String email,
        @NotNull PerfilUsuario perfil) {
}
