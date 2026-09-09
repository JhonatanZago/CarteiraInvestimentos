package com.example.carteirainvestimento.service;

import com.example.carteirainvestimento.domain.AtivoCarteira;
import com.example.carteirainvestimento.domain.PortfolioSnapshot;
import com.example.carteirainvestimento.dto.insight.DisponibilidadeInsight;
import com.example.carteirainvestimento.dto.insight.IndicadorMercadoResponse;
import com.example.carteirainvestimento.dto.insight.PontoEvolucaoCarteiraResponse;
import com.example.carteirainvestimento.dto.insight.ResumoProventosResponse;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.integration.adapter.MarketIndicatorAdapter;
import com.example.carteirainvestimento.integration.adapter.YahooFinanceIncomeAdapter;
import com.example.carteirainvestimento.integration.adapter.BrapiIncomeAdapter;
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.repository.CarteiraRepository;
import com.example.carteirainvestimento.repository.PortfolioSnapshotRepository;
import com.example.carteirainvestimento.repository.HistoricoCotacaoRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MarketInsightService {
    private static final Logger log = LoggerFactory.getLogger(MarketInsightService.class);
    private static final Duration TTL_INDICADORES = Duration.ofMinutes(5);
    private final CarteiraRepository carteiras;
    private final AtivoCarteiraRepository ativos;
    private final List<MarketIndicatorAdapter> indicadoresAdapters;
    private final PortfolioSnapshotRepository snapshots;
    private final HistoricoCotacaoRepository historicoCotacoes;
    private final YahooFinanceIncomeAdapter proventosAdapter;
    private final BrapiIncomeAdapter brapiIncomeAdapter;
    private final AtomicReference<IndicadoresEmCache> cache = new AtomicReference<>();

    public MarketInsightService(CarteiraRepository carteiras, AtivoCarteiraRepository ativos, List<MarketIndicatorAdapter> indicadoresAdapters, PortfolioSnapshotRepository snapshots, HistoricoCotacaoRepository historicoCotacoes, YahooFinanceIncomeAdapter proventosAdapter, BrapiIncomeAdapter brapiIncomeAdapter) {
        this.carteiras = carteiras; this.ativos = ativos; this.indicadoresAdapters = indicadoresAdapters; this.snapshots = snapshots; this.historicoCotacoes = historicoCotacoes; this.proventosAdapter = proventosAdapter; this.brapiIncomeAdapter = brapiIncomeAdapter;
    }
    public List<IndicadorMercadoResponse> indicadores() {
        IndicadoresEmCache atual = cache.get();
        if (atual != null && atual.criadoEm().plus(TTL_INDICADORES).isAfter(OffsetDateTime.now())) return atual.indicadores();
        try { List<IndicadorMercadoResponse> novos = consolidarIndicadores(); cache.set(new IndicadoresEmCache(novos, OffsetDateTime.now())); return novos; }
        catch (RuntimeException ex) { return atual == null ? indisponiveis() : atual.indicadores().stream().map(this::marcarDesatualizado).toList(); }
    }
    private List<IndicadorMercadoResponse> consolidarIndicadores() {
        Map<String, IndicadorMercadoResponse> porCodigo = new LinkedHashMap<>();
        for (MarketIndicatorAdapter adapter : indicadoresAdapters) {
            try { for (IndicadorMercadoResponse item : adapter.buscarIndicadores()) { if (!porCodigo.containsKey(item.codigo()) || porCodigo.get(item.codigo()).valor() == null && item.valor() != null) porCodigo.put(item.codigo(), item); } }
            catch (RuntimeException ignored) { }
        }
        return porCodigo.isEmpty() ? indisponiveis() : new ArrayList<>(porCodigo.values());
    }
    @Transactional(readOnly = true)
    public List<PontoEvolucaoCarteiraResponse> evolucao(Long carteiraId) {
        validarCarteira(carteiraId);
        // A evolução representa exclusivamente snapshots financeiros reais.
        // Cotações históricas de ativos não são convertidas em patrimônio
        // retroativo, pois isso fabricaria aportes/posições inexistentes.
        return snapshots.findByCarteiraIdOrderByDataHoraAsc(carteiraId).stream()
                .sorted(java.util.Comparator.comparing(PortfolioSnapshot::getDataHora))
                .map(this::ponto).toList();
    }
    @Transactional(readOnly = true)
    public ResumoProventosResponse proventos(Long carteiraId) {
        validarCarteira(carteiraId); List<AtivoCarteira> posicoes = ativos.findByCarteiraId(carteiraId);
        if (posicoes.isEmpty()) return new ResumoProventosResponse(BigDecimal.ZERO.setScale(2), null, null, DisponibilidadeInsight.AVAILABLE);
        OffsetDateTime now = OffsetDateTime.now(); BigDecimal estimado = BigDecimal.ZERO; boolean consultado = false; boolean falhou = false;
        Map<String, List<YahooFinanceIncomeAdapter.DividendEvent>> brapiEventos = new java.util.HashMap<>();
        try {
            // Consultar apenas ativos com moeda/listagem brasileira na fonte
            // brasileira; tickers internacionais sem cobertura não devem
            // provocar rate-limit da consulta dos ativos válidos.
            List<String> tickersBrasil = posicoes.stream()
                    .filter(p -> p.getAcao().getMoeda() == com.example.carteirainvestimento.enums.Moeda.BRL)
                    .map(p -> p.getAcao().getTicker()).distinct().toList();
            brapiEventos.putAll(brapiIncomeAdapter.buscarDividendos(tickersBrasil)); consultado = true;
        } catch (RuntimeException ex) { falhou = true; }
        for (AtivoCarteira posicao : posicoes) {
            try {
                List<YahooFinanceIncomeAdapter.DividendEvent> eventos = brapiEventos.get(posicao.getAcao().getTicker());
                // A consulta BRAPI já retorna cobertura parcial por ticker. Não
                // encadear uma chamada Yahoo lenta para cada ativo sem cobertura;
                // isso bloqueava o carregamento do dashboard inteiro.
                if (eventos == null) eventos = List.of();
                for (YahooFinanceIncomeAdapter.DividendEvent evento : eventos) if (!posicao.getDataPrimeiraCompra().isAfter(evento.dataPagamento().toLocalDate())) estimado = estimado.add(evento.valorPorAcao().multiply(posicao.getQuantidade()));
            } catch (RuntimeException ex) { falhou = true; log.warn("Proventos indisponíveis para {}: {}", posicao.getAcao().getTicker(), ex.getClass().getSimpleName()); }
        }
        if (!consultado && falhou) return new ResumoProventosResponse(BigDecimal.ZERO.setScale(2), null, null, DisponibilidadeInsight.UNAVAILABLE);
        return new ResumoProventosResponse(BigDecimal.ZERO.setScale(2), CalculadoraFinanceira.monetario(estimado), now, falhou ? DisponibilidadeInsight.STALE : DisponibilidadeInsight.AVAILABLE);
    }
    private List<IndicadorMercadoResponse> indisponiveis() { return List.of(indisponivel("IBOV", "Ibovespa"), indisponivel("CDI", "CDI"), indisponivel("USD", "Dolar comercial")); }
    private PontoEvolucaoCarteiraResponse ponto(PortfolioSnapshot snapshot) { return new PontoEvolucaoCarteiraResponse(snapshot.getDataHora(), snapshot.getTotalInvestido(), snapshot.getPatrimonioAtual(), DisponibilidadeInsight.AVAILABLE, snapshot.getDataHora(), snapshot.getTotalInvestido(), snapshot.getPatrimonioAtual(), snapshot.getResultado(), snapshot.getRentabilidade()); }
    private IndicadorMercadoResponse indisponivel(String codigo, String descricao) { return new IndicadorMercadoResponse(codigo, descricao, null, null, null, DisponibilidadeInsight.UNAVAILABLE); }
    private IndicadorMercadoResponse marcarDesatualizado(IndicadorMercadoResponse indicador) { return new IndicadorMercadoResponse(indicador.codigo(), indicador.descricao(), indicador.valor(), indicador.variacaoPercentual(), indicador.referenciaEm(), DisponibilidadeInsight.STALE); }
    private void validarCarteira(Long id) { if (!carteiras.existsById(id)) throw new ResourceNotFoundException("Carteira nao encontrada"); }
    private record IndicadoresEmCache(List<IndicadorMercadoResponse> indicadores, OffsetDateTime criadoEm) { }
}
