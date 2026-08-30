package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.dto.insight.DisponibilidadeInsight;
import com.example.carteirainvestimento.dto.insight.IndicadorMercadoResponse;
import java.util.List;
import org.springframework.stereotype.Component;

/** Safe default until a configured provider adapter is enabled. */
@Component
public class UnavailableMarketIndicatorAdapter implements MarketIndicatorAdapter {
    @Override public List<IndicadorMercadoResponse> buscarIndicadores() {
        return List.of(indisponivel("IBOV", "Ibovespa"), indisponivel("CDI", "CDI"), indisponivel("USD", "Dolar comercial"));
    }
    private IndicadorMercadoResponse indisponivel(String codigo, String descricao) {
        return new IndicadorMercadoResponse(codigo, descricao, null, null, null, DisponibilidadeInsight.UNAVAILABLE);
    }
}
