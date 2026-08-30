package com.example.carteirainvestimento.integration.dto;

public record EmpresaConsulta(
        String cnpj,
        String razaoSocial,
        String nomeFantasia,
        String email,
        String telefone,
        String situacaoCadastral) {
}
