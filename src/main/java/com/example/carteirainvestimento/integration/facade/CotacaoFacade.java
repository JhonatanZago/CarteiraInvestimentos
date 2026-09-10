package com.example.carteirainvestimento.integration.facade;

import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.integration.adapter.CotacaoAdapter;
import com.example.carteirainvestimento.integration.adapter.TwelveDataCotacaoAdapter;
import com.example.carteirainvestimento.integration.dto.CotacaoConsulta;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CotacaoFacade {
    private final List<CotacaoAdapter> adapters;
    public CotacaoFacade(List<CotacaoAdapter> adapters) { this.adapters = List.copyOf(adapters); }
    public CotacaoConsulta buscarCotacao(String ticker, Mercado mercado) {
        var candidatas = adapters.stream().filter(adapter -> adapter.suporta(mercado))
                .sorted((a, b) -> Boolean.compare(!(a instanceof TwelveDataCotacaoAdapter), !(b instanceof TwelveDataCotacaoAdapter)))
                .toList();
        if (candidatas.isEmpty()) throw new BusinessRuleException("Mercado sem fonte configurada");
        return candidatas.getFirst().buscarCotacao(ticker);
    }
}
