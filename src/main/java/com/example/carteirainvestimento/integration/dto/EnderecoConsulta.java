package com.example.carteirainvestimento.integration.dto;

public record EnderecoConsulta(
        String cep,
        String logradouro,
        String bairro,
        String cidade,
        String uf) {
}
