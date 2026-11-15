package com.example.carteirainvestimento.dto.admin;

import jakarta.validation.constraints.NotNull;

public record AdminStatusRequest(@NotNull Boolean ativo) {
}
