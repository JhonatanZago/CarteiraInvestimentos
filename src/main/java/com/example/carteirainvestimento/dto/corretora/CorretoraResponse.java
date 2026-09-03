package com.example.carteirainvestimento.dto.corretora;

import java.time.OffsetDateTime;

import com.example.carteirainvestimento.domain.StatusValidacaoCorretora;

public record CorretoraResponse(
        Long id, String cnpj, String razaoSocial, String nomeFantasia, String email, String telefone,
        String cep, String logradouro, String numero, String complemento, String bairro, String cidade, String uf,
        String situacaoCadastral, boolean validadaMercadoFinanceiro, OffsetDateTime dataValidacaoMercado,
        String fonteValidacaoMercado, StatusValidacaoCorretora statusValidacao, String motivoValidacao,
        OffsetDateTime dataCadastro) {
}
