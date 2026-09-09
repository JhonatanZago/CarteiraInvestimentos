package com.example.carteirainvestimento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.example.carteirainvestimento.domain.Carteira;
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
