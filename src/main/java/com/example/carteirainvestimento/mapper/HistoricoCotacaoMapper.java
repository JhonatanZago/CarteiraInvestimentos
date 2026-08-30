package com.example.carteirainvestimento.mapper;

import com.example.carteirainvestimento.domain.HistoricoCotacao;
import com.example.carteirainvestimento.dto.historico.HistoricoCotacaoResponse;

public final class HistoricoCotacaoMapper {

    private HistoricoCotacaoMapper() {
    }

    public static HistoricoCotacaoResponse toResponse(HistoricoCotacao historico) {
        return new HistoricoCotacaoResponse(historico.getId(), historico.getAcao().getId(), historico.getValor(),
                historico.getDataHoraCotacao(), historico.getDataHoraRegistro(), historico.getFonte());
    }
}
