package com.example.carteirainvestimento.mapper;

import com.example.carteirainvestimento.domain.Corretora;
import com.example.carteirainvestimento.dto.corretora.CorretoraResponse;

public final class CorretoraMapper {

    private CorretoraMapper() {
    }

    public static CorretoraResponse toResponse(Corretora corretora) {
        return new CorretoraResponse(corretora.getId(), corretora.getCnpj(), corretora.getRazaoSocial(),
                corretora.getNomeFantasia(), corretora.getEmail(), corretora.getTelefone(), corretora.getCep(),
                corretora.getLogradouro(), corretora.getNumero(), corretora.getComplemento(), corretora.getBairro(),
                corretora.getCidade(), corretora.getUf(), corretora.getSituacaoCadastral(),
                corretora.isValidadaMercadoFinanceiro(), corretora.getDataValidacaoMercado(),
                corretora.getFonteValidacaoMercado(), corretora.getDataCadastro());
    }
}
