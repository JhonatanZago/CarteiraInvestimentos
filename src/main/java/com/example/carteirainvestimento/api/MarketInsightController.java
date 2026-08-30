package com.example.carteirainvestimento.api;

import com.example.carteirainvestimento.dto.insight.IndicadorMercadoResponse;
import com.example.carteirainvestimento.dto.insight.PontoEvolucaoCarteiraResponse;
import com.example.carteirainvestimento.dto.insight.ResumoProventosResponse;
import com.example.carteirainvestimento.service.MarketInsightService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MarketInsightController {
    private final MarketInsightService insights;
    public MarketInsightController(MarketInsightService insights) { this.insights = insights; }
    @GetMapping("/mercado/indicadores") public List<IndicadorMercadoResponse> indicadores() { return insights.indicadores(); }
    @GetMapping("/carteiras/{carteiraId}/evolucao") public List<PontoEvolucaoCarteiraResponse> evolucao(@PathVariable Long carteiraId) { return insights.evolucao(carteiraId); }
    @GetMapping("/carteiras/{carteiraId}/proventos") public ResumoProventosResponse proventos(@PathVariable Long carteiraId) { return insights.proventos(carteiraId); }
}
