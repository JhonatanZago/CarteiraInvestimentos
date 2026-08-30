package com.example.carteirainvestimento.service;

import com.example.carteirainvestimento.domain.AtivoCarteira;
import com.example.carteirainvestimento.dto.insight.DisponibilidadeInsight;
import com.example.carteirainvestimento.dto.insight.IndicadorMercadoResponse;
import com.example.carteirainvestimento.dto.insight.PontoEvolucaoCarteiraResponse;
import com.example.carteirainvestimento.dto.insight.ResumoProventosResponse;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.integration.adapter.MarketIndicatorAdapter;
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.repository.CarteiraRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketInsightService {
    private static final Duration TTL_INDICADORES = Duration.ofMinutes(5);
    private final CarteiraRepository carteiras;
    private final AtivoCarteiraRepository ativos;
    private final MarketIndicatorAdapter indicadoresAdapter;
    private final AtomicReference<IndicadoresEmCache> cache = new AtomicReference<>();

    public MarketInsightService(CarteiraRepository carteiras, AtivoCarteiraRepository ativos, MarketIndicatorAdapter indicadoresAdapter) {
        this.carteiras = carteiras; this.ativos = ativos; this.indicadoresAdapter = indicadoresAdapter;
    }
    public List<IndicadorMercadoResponse> indicadores() {
        IndicadoresEmCache atual = cache.get();
        if (atual != null && atual.criadoEm().plus(TTL_INDICADORES).isAfter(OffsetDateTime.now())) return atual.indicadores();
        try { List<IndicadorMercadoResponse> novos = indicadoresAdapter.buscarIndicadores(); cache.set(new IndicadoresEmCache(novos, OffsetDateTime.now())); return novos; }
        catch (RuntimeException ex) { return atual == null ? indisponiveis() : atual.indicadores().stream().map(this::marcarDesatualizado).toList(); }
    }
    @Transactional(readOnly = true)
    public List<PontoEvolucaoCarteiraResponse> evolucao(Long carteiraId) {
        validarCarteira(carteiraId); List<AtivoCarteira> posicoes = ativos.findByCarteiraId(carteiraId);
        if (posicoes.isEmpty()) return List.of();
        BigDecimal investido = posicoes.stream().map(p -> p.getQuantidade().multiply(p.getPrecoMedio())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal atual = posicoes.stream().map(p -> p.getQuantidade().multiply(p.getAcao().getCotacaoAtual())).reduce(BigDecimal.ZERO, BigDecimal::add);
        OffsetDateTime referencia = posicoes.stream().map(p -> p.getAcao().getDataHoraCotacao()).filter(java.util.Objects::nonNull).max(OffsetDateTime::compareTo).orElse(null);
        return List.of(new PontoEvolucaoCarteiraResponse(referencia, CalculadoraFinanceira.monetario(investido), CalculadoraFinanceira.monetario(atual), DisponibilidadeInsight.AVAILABLE));
    }
    @Transactional(readOnly = true)
    public ResumoProventosResponse proventos(Long carteiraId) { validarCarteira(carteiraId); return new ResumoProventosResponse(BigDecimal.ZERO.setScale(2), null, null, DisponibilidadeInsight.UNAVAILABLE); }
    private List<IndicadorMercadoResponse> indisponiveis() { return List.of(indisponivel("IBOV", "Ibovespa"), indisponivel("CDI", "CDI"), indisponivel("USD", "Dolar comercial")); }
    private IndicadorMercadoResponse indisponivel(String codigo, String descricao) { return new IndicadorMercadoResponse(codigo, descricao, null, null, null, DisponibilidadeInsight.UNAVAILABLE); }
    private IndicadorMercadoResponse marcarDesatualizado(IndicadorMercadoResponse indicador) { return new IndicadorMercadoResponse(indicador.codigo(), indicador.descricao(), indicador.valor(), indicador.variacaoPercentual(), indicador.referenciaEm(), DisponibilidadeInsight.STALE); }
    private void validarCarteira(Long id) { if (!carteiras.existsById(id)) throw new ResourceNotFoundException("Carteira nao encontrada"); }
    private record IndicadoresEmCache(List<IndicadorMercadoResponse> indicadores, OffsetDateTime criadoEm) { }
}
