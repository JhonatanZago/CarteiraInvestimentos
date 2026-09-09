package com.example.carteirainvestimento.dto.corretora;

import java.time.OffsetDateTime;

import com.example.carteirainvestimento.domain.StatusValidacaoCorretora;

public record CorretoraResponse(
        Long id, String cnpj, String razaoSocial, String nomeFantasia, String email, String telefone,
        String cep, String logradouro, String numero, String complemento, String bairro, String cidade, String uf,
        String situacaoCadastral, boolean validadaMercadoFinanceiro, OffsetDateTime dataValidacaoMercado,
        String fonteValidacaoMercado, StatusValidacaoCorretora statusValidacao, String motivoValidacao,
        OffsetDateTime dataCadastro, String logoUrl, String logoSource, OffsetDateTime logoUpdatedAt,
        String website, String statusLogo) {
    public CorretoraResponse(Long id, String cnpj, String razaoSocial, String nomeFantasia, String email,
            String telefone, String cep, String logradouro, String numero, String complemento, String bairro,
            String cidade, String uf, String situacaoCadastral, boolean validadaMercadoFinanceiro,
            OffsetDateTime dataValidacaoMercado, String fonteValidacaoMercado,
            StatusValidacaoCorretora statusValidacao, String motivoValidacao, OffsetDateTime dataCadastro) {
        this(id, cnpj, razaoSocial, nomeFantasia, email, telefone, cep, logradouro, numero, complemento, bairro,
                cidade, uf, situacaoCadastral, validadaMercadoFinanceiro, dataValidacaoMercado,
                fonteValidacaoMercado, statusValidacao, motivoValidacao, dataCadastro, null, null, null, null, null);
    }
}
