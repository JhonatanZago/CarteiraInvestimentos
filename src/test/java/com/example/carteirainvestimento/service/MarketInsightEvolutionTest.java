package com.example.carteirainvestimento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.example.carteirainvestimento.domain.Carteira;
import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.domain.AtivoCarteira;
import com.example.carteirainvestimento.domain.PortfolioSnapshot;
import com.example.carteirainvestimento.dto.insight.DisponibilidadeInsight;
import com.example.carteirainvestimento.integration.adapter.BrapiIncomeAdapter;
import com.example.carteirainvestimento.integration.adapter.MarketIndicatorAdapter;
import com.example.carteirainvestimento.integration.adapter.YahooFinanceIncomeAdapter;
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.repository.CarteiraRepository;
import com.example.carteirainvestimento.repository.HistoricoCotacaoRepository;
import com.example.carteirainvestimento.repository.PortfolioSnapshotRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MarketInsightEvolutionTest {
    @Test
    void returnsOnlyChronologicalRealSnapshots() {
        CarteiraRepository carteiras = mock(CarteiraRepository.class);
        PortfolioSnapshotRepository snapshots = mock(PortfolioSnapshotRepository.class);
        when(carteiras.existsById(7L)).thenReturn(true);
        when(snapshots.findByCarteiraIdOrderByDataHoraAsc(7L)).thenReturn(List.of(
                snapshot("2026-09-02T10:00:00Z", "100", "105"),
                snapshot("2026-09-01T10:00:00Z", "100", "98")));
        MarketInsightService service = new MarketInsightService(carteiras, mock(AtivoCarteiraRepository.class),
                List.<MarketIndicatorAdapter>of(), snapshots, mock(HistoricoCotacaoRepository.class),
                mock(YahooFinanceIncomeAdapter.class), mock(BrapiIncomeAdapter.class));

        var result = service.evolucao(7L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).referenciaEm()).isBefore(result.get(1).referenciaEm());
        assertThat(result.get(0).resultAmount()).isEqualByComparingTo("-2");
        assertThat(result.get(1).resultAmount()).isEqualByComparingTo("5");
    }

    @Test
    void returnsEmptyWhenNoSnapshotsExist() {
        CarteiraRepository carteiras = mock(CarteiraRepository.class);
        PortfolioSnapshotRepository snapshots = mock(PortfolioSnapshotRepository.class);
        when(carteiras.existsById(7L)).thenReturn(true);
        when(snapshots.findByCarteiraIdOrderByDataHoraAsc(7L)).thenReturn(List.of());
        MarketInsightService service = new MarketInsightService(carteiras, mock(AtivoCarteiraRepository.class),
                List.<MarketIndicatorAdapter>of(), snapshots, mock(HistoricoCotacaoRepository.class),
                mock(YahooFinanceIncomeAdapter.class), mock(BrapiIncomeAdapter.class));

        assertThat(service.evolucao(7L)).isEmpty();
    }

    @Test
    void consolidatesIndicatorsAndUsesCache() {
        CarteiraRepository carteiras = mock(CarteiraRepository.class);
        MarketIndicatorAdapter source = () -> List.of(
                new com.example.carteirainvestimento.dto.insight.IndicadorMercadoResponse(
                        "IBOV", "Ibovespa", new BigDecimal("130000"), null,
                        OffsetDateTime.now(), DisponibilidadeInsight.AVAILABLE));
        MarketInsightService service = new MarketInsightService(carteiras, mock(AtivoCarteiraRepository.class),
                List.of(source), mock(PortfolioSnapshotRepository.class), mock(HistoricoCotacaoRepository.class),
                mock(YahooFinanceIncomeAdapter.class), mock(BrapiIncomeAdapter.class));

        assertThat(service.indicadores()).hasSize(1);
        assertThat(service.indicadores().getFirst().valor()).isEqualByComparingTo("130000");
    }

    @Test
    void returnsUnavailableIndicatorsWhenAllSourcesFail() {
        MarketIndicatorAdapter failing = () -> { throw new IllegalStateException("provider down"); };
        MarketInsightService service = new MarketInsightService(mock(CarteiraRepository.class), mock(AtivoCarteiraRepository.class),
                List.of(failing), mock(PortfolioSnapshotRepository.class), mock(HistoricoCotacaoRepository.class),
                mock(YahooFinanceIncomeAdapter.class), mock(BrapiIncomeAdapter.class));

        assertThat(service.indicadores()).allMatch(item -> item.disponibilidade() == DisponibilidadeInsight.UNAVAILABLE);
    }

    @Test
    void summarizesBrazilianDividendsAndDoesNotQueryYahooForMissingCoverage() {
        CarteiraRepository carteiras = mock(CarteiraRepository.class);
        AtivoCarteiraRepository ativos = mock(AtivoCarteiraRepository.class);
        BrapiIncomeAdapter brapi = mock(BrapiIncomeAdapter.class);
        YahooFinanceIncomeAdapter yahoo = mock(YahooFinanceIncomeAdapter.class);
        Acao acao = new Acao();
        acao.setTicker("PETR4");
        acao.setMoeda(com.example.carteirainvestimento.enums.Moeda.BRL);
        AtivoCarteira position = new AtivoCarteira();
        position.setAcao(acao);
        position.setQuantidade(new BigDecimal("10"));
        position.setDataPrimeiraCompra(java.time.LocalDate.of(2026, 1, 1));
        when(carteiras.existsById(7L)).thenReturn(true);
        when(ativos.findByCarteiraId(7L)).thenReturn(List.of(position));
        when(brapi.buscarDividendos(List.of("PETR4"))).thenReturn(Map.of("PETR4", List.of(
                new YahooFinanceIncomeAdapter.DividendEvent("PETR4", new BigDecimal("0.50"),
                        OffsetDateTime.parse("2026-09-04T00:00:00Z")))));

        MarketInsightService service = new MarketInsightService(carteiras, ativos, List.of(),
                mock(PortfolioSnapshotRepository.class), mock(HistoricoCotacaoRepository.class),
                yahoo, brapi);

        var result = service.proventos(7L);

        assertThat(result.disponibilidade()).isEqualTo(DisponibilidadeInsight.AVAILABLE);
        assertThat(result.proximosProventos()).isEqualByComparingTo("5.00");
        verifyNoInteractions(yahoo);
    }

    @Test
    void returnsAvailableZeroWhenPortfolioHasNoPositions() {
        CarteiraRepository carteiras = mock(CarteiraRepository.class);
        AtivoCarteiraRepository ativos = mock(AtivoCarteiraRepository.class);
        when(carteiras.existsById(8L)).thenReturn(true);
        when(ativos.findByCarteiraId(8L)).thenReturn(List.of());
        MarketInsightService service = new MarketInsightService(carteiras, ativos, List.of(),
                mock(PortfolioSnapshotRepository.class), mock(HistoricoCotacaoRepository.class),
                mock(YahooFinanceIncomeAdapter.class), mock(BrapiIncomeAdapter.class));

        var result = service.proventos(8L);

        assertThat(result.disponibilidade()).isEqualTo(DisponibilidadeInsight.AVAILABLE);
        assertThat(result.recebidosUltimosDozeMeses()).isEqualByComparingTo("0.00");
        assertThat(result.proximosProventos()).isNull();
    }

    @Test
    void rejectsEvolutionForUnknownPortfolio() {
        CarteiraRepository carteiras = mock(CarteiraRepository.class);
        when(carteiras.existsById(99L)).thenReturn(false);
        MarketInsightService service = new MarketInsightService(carteiras, mock(AtivoCarteiraRepository.class), List.of(),
                mock(PortfolioSnapshotRepository.class), mock(HistoricoCotacaoRepository.class),
                mock(YahooFinanceIncomeAdapter.class), mock(BrapiIncomeAdapter.class));

        assertThatThrownBy(() -> service.evolucao(99L))
                .isInstanceOf(com.example.carteirainvestimento.exception.ResourceNotFoundException.class);
    }

    @Test
    void marksDividendsUnavailableWhenProviderFails() {
        CarteiraRepository carteiras = mock(CarteiraRepository.class);
        AtivoCarteiraRepository ativos = mock(AtivoCarteiraRepository.class);
        BrapiIncomeAdapter brapi = mock(BrapiIncomeAdapter.class);
        Acao acao = new Acao();
        acao.setTicker("PETR4");
        acao.setMoeda(com.example.carteirainvestimento.enums.Moeda.BRL);
        AtivoCarteira position = new AtivoCarteira();
        position.setAcao(acao);
        position.setQuantidade(BigDecimal.ONE);
        position.setDataPrimeiraCompra(java.time.LocalDate.of(2026, 1, 1));
        when(carteiras.existsById(9L)).thenReturn(true);
        when(ativos.findByCarteiraId(9L)).thenReturn(List.of(position));
        when(brapi.buscarDividendos(List.of("PETR4"))).thenThrow(new IllegalStateException("provider down"));
        MarketInsightService service = new MarketInsightService(carteiras, ativos, List.of(),
                mock(PortfolioSnapshotRepository.class), mock(HistoricoCotacaoRepository.class),
                mock(YahooFinanceIncomeAdapter.class), brapi);

        var result = service.proventos(9L);

        assertThat(result.disponibilidade()).isEqualTo(DisponibilidadeInsight.UNAVAILABLE);
    }

    private PortfolioSnapshot snapshot(String timestamp, String invested, String current) {
        PortfolioSnapshot snapshot = new PortfolioSnapshot();
        snapshot.setDataHora(OffsetDateTime.parse(timestamp).withOffsetSameInstant(ZoneOffset.UTC));
        snapshot.setTotalInvestido(new BigDecimal(invested));
        snapshot.setPatrimonioAtual(new BigDecimal(current));
        snapshot.setResultado(new BigDecimal(current).subtract(new BigDecimal(invested)));
        snapshot.setRentabilidade(new BigDecimal(current).subtract(new BigDecimal(invested)));
        snapshot.setOrigem(com.example.carteirainvestimento.enums.OrigemSnapshotCarteira.DAILY_CLOSE);
        return snapshot;
    }
}
