package com.example.carteirainvestimento.dto.corretora;

import java.time.OffsetDateTime;

public record CorretoraResponse(
        Long id, String cnpj, String razaoSocial, String nomeFantasia, String email, String telefone,
        String cep, String logradouro, String numero, String complemento, String bairro, String cidade, String uf,
        String situacaoCadastral, boolean validadaMercadoFinanceiro, OffsetDateTime dataValidacaoMercado,
        String fonteValidacaoMercado, OffsetDateTime dataCadastro) {
}
