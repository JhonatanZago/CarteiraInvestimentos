package com.example.carteirainvestimento.integration.facade;

import java.util.List;

import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.integration.adapter.CotacaoAdapter;
import com.example.carteirainvestimento.integration.dto.CotacaoConsulta;
import org.springframework.stereotype.Component;

@Component
public class CotacaoFacade {

    private final List<CotacaoAdapter> adapters;

    public CotacaoFacade(List<CotacaoAdapter> adapters) {
        this.adapters = List.copyOf(adapters);
    }

    public CotacaoConsulta buscarCotacao(String ticker, Mercado mercado) {
        return adapters.stream()
                .filter(adapter -> adapter.suporta(mercado))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Mercado sem fonte de cotaÃ§Ã£o configurada"))
                .buscarCotacao(ticker);
    }
}
