package com.example.carteirainvestimento.dto.carteira;

import jakarta.validation.constraints.NotBlank;

public record CarteiraRequest(@NotBlank String nome, String descricao) {
}
