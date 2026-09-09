package com.example.carteirainvestimento.mapper;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.dto.acao.AcaoResponse;

public final class AcaoMapper {

    private AcaoMapper() {
    }

    public static AcaoResponse toResponse(Acao acao) {
        return new AcaoResponse(acao.getId(), acao.getTicker(), acao.getNomeEmpresa(), acao.getMercado(),
                acao.getMoeda(), acao.getCotacaoAtual(), acao.getDataHoraCotacao(), acao.getLogoUrl(),
                acao.getListingCountryCode(), acao.getExchange(), acao.getExchangeMic(), acao.getDataSource());
    }
}
