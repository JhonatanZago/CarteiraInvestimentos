package com.example.carteirainvestimento.dto.carteira;

import java.time.OffsetDateTime;

public record CarteiraResponse(Long id, String nome, String descricao, OffsetDateTime dataCriacao) {
}
