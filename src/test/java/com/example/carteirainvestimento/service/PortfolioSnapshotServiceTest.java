package com.example.carteirainvestimento.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.domain.AtivoCarteira;
import com.example.carteirainvestimento.domain.Carteira;
import com.example.carteirainvestimento.domain.PortfolioSnapshot;
import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.enums.OrigemSnapshotCarteira;
import com.example.carteirainvestimento.dto.financeiro.ContextoMonetario;
import com.example.carteirainvestimento.dto.financeiro.DisponibilidadeMonetaria;
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.repository.CarteiraRepository;
import com.example.carteirainvestimento.repository.PortfolioSnapshotRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

class PortfolioSnapshotServiceTest {
    @Test
    void dailyCloseUpdatesExistingDayInsteadOfDuplicating() {
        Carteira carteira = new Carteira();
        carteira.setId(1L);
        Acao acao = new Acao();
        acao.setMoeda(Moeda.BRL);
        acao.setMercado(Mercado.BRASIL);
        acao.setCotacaoAtual(new BigDecimal("12.00"));
        AtivoCarteira posicao = new AtivoCarteira();
        posicao.setQuantidade(new BigDecimal("10"));
        posicao.setPrecoMedio(new BigDecimal("10"));
        posicao.setAcao(acao);

        CarteiraRepository carteiras = mock(CarteiraRepository.class);
        AtivoCarteiraRepository ativos = mock(AtivoCarteiraRepository.class);
        PortfolioSnapshotRepository snapshots = mock(PortfolioSnapshotRepository.class);
        when(carteiras.findAll()).thenReturn(List.of(carteira));
        when(carteiras.getReferenceById(1L)).thenReturn(carteira);
        when(ativos.findByCarteiraId(1L)).thenReturn(List.of(posicao));
        when(snapshots.findFirstByCarteiraIdAndOrigemAndDataHoraBetween(eq(1L), eq(OrigemSnapshotCarteira.DAILY_CLOSE), any(), any()))
                .thenReturn(Optional.empty());

        PortfolioSnapshotService service = new PortfolioSnapshotService(carteiras, ativos, snapshots);
        service.registrarFechamentoDiario(LocalDate.of(2026, 9, 8));
        verify(snapshots, times(1)).save(any(PortfolioSnapshot.class));

        PortfolioSnapshot existente = new PortfolioSnapshot();
        when(snapshots.findFirstByCarteiraIdAndOrigemAndDataHoraBetween(eq(1L), eq(OrigemSnapshotCarteira.DAILY_CLOSE), any(), any()))
                .thenReturn(Optional.of(existente));
        service.registrarFechamentoDiario(LocalDate.of(2026, 9, 8));
        verify(snapshots, times(2)).save(any(PortfolioSnapshot.class));
    }

    @Test
    void persistsBaseValuesAndExactFxProvenanceForMixedPortfolio() {
        Carteira carteira = new Carteira();
        carteira.setId(1L);
        AtivoCarteira brl = position(Moeda.BRL, "2", "10", "12");
        AtivoCarteira usd = position(Moeda.USD, "1", "10", "12");
        CarteiraRepository carteiras = mock(CarteiraRepository.class);
        AtivoCarteiraRepository ativos = mock(AtivoCarteiraRepository.class);
        PortfolioSnapshotRepository snapshots = mock(PortfolioSnapshotRepository.class);
        ExchangeRateService exchangeRates = mock(ExchangeRateService.class);
        when(carteiras.getReferenceById(1L)).thenReturn(carteira);
        when(ativos.findByCarteiraId(1L)).thenReturn(List.of(brl, usd));
        when(snapshots.findTopByCarteiraIdOrderByDataHoraDesc(1L)).thenReturn(Optional.empty());
        when(exchangeRates.obterTaxa(Moeda.BRL, Moeda.BRL)).thenReturn(identityContext());
        when(exchangeRates.obterTaxa(Moeda.USD, Moeda.BRL)).thenReturn(ContextoMonetario.available(
                Moeda.USD, Moeda.BRL, BigDecimal.ONE, new BigDecimal("5"), new BigDecimal("5"),
                java.time.OffsetDateTime.parse("2026-11-15T11:00:00Z"), "TEST-FX"));

        new PortfolioSnapshotService(carteiras, ativos, snapshots, exchangeRates)
                .registrarEvento(1L, OrigemSnapshotCarteira.POSITION_CREATED);

        ArgumentCaptor<PortfolioSnapshot> captor = ArgumentCaptor.forClass(PortfolioSnapshot.class);
        verify(snapshots).save(captor.capture());
        PortfolioSnapshot snapshot = captor.getValue();
        assertThat(snapshot.getMoedaBase()).isEqualTo(Moeda.BRL);
        assertThat(snapshot.getTotalInvestidoBase()).isEqualByComparingTo("70.00000000");
        assertThat(snapshot.getPatrimonioAtualBase()).isEqualByComparingTo("84.00000000");
        assertThat(snapshot.getResultadoBase()).isEqualByComparingTo("14.00000000");
        assertThat(snapshot.getDisponibilidadeMonetaria()).isEqualTo(DisponibilidadeMonetaria.AVAILABLE);
        assertThat(snapshot.getCambios()).hasSize(2);
        assertThat(snapshot.getCambios()).anyMatch(c -> c.getMoedaOriginal() == Moeda.USD
                && c.getTaxaCambio().compareTo(new BigDecimal("5")) == 0
                && c.getFonteCambio().equals("TEST-FX"));
    }

    @Test
    void keepsExistingSnapshotBaseValuesWhenLaterRateChanges() {
        Carteira carteira = new Carteira();
        carteira.setId(1L);
        AtivoCarteira usd = position(Moeda.USD, "1", "10", "12");
        PortfolioSnapshot existing = new PortfolioSnapshot();
        existing.setTotalInvestido(new BigDecimal("10.0000"));
        existing.setPatrimonioAtual(new BigDecimal("12.0000"));
        existing.setTotalInvestidoBase(new BigDecimal("50.00000000"));
        existing.setPatrimonioAtualBase(new BigDecimal("60.00000000"));
        existing.setDisponibilidadeMonetaria(DisponibilidadeMonetaria.AVAILABLE);
        CarteiraRepository carteiras = mock(CarteiraRepository.class);
        AtivoCarteiraRepository ativos = mock(AtivoCarteiraRepository.class);
        PortfolioSnapshotRepository snapshots = mock(PortfolioSnapshotRepository.class);
        ExchangeRateService exchangeRates = mock(ExchangeRateService.class);
        when(carteiras.getReferenceById(1L)).thenReturn(carteira);
        when(ativos.findByCarteiraId(1L)).thenReturn(List.of(usd));
        when(snapshots.findTopByCarteiraIdOrderByDataHoraDesc(1L)).thenReturn(Optional.of(existing));

        new PortfolioSnapshotService(carteiras, ativos, snapshots, exchangeRates)
                .registrarEvento(1L, OrigemSnapshotCarteira.QUOTE_UPDATED);

        verify(snapshots, never()).save(any(PortfolioSnapshot.class));
        assertThat(existing.getTotalInvestidoBase()).isEqualByComparingTo("50");
        assertThat(existing.getPatrimonioAtualBase()).isEqualByComparingTo("60");
    }

    @Test
    void persistsEmptySnapshotWithZeroAvailableBaseValues() {
        Carteira carteira = new Carteira(); carteira.setId(1L);
        CarteiraRepository carteiras = mock(CarteiraRepository.class);
        AtivoCarteiraRepository ativos = mock(AtivoCarteiraRepository.class);
        PortfolioSnapshotRepository snapshots = mock(PortfolioSnapshotRepository.class);
        when(carteiras.getReferenceById(1L)).thenReturn(carteira);
        when(ativos.findByCarteiraId(1L)).thenReturn(List.of());
        when(snapshots.findTopByCarteiraIdOrderByDataHoraDesc(1L)).thenReturn(Optional.empty());

        new PortfolioSnapshotService(carteiras, ativos, snapshots)
                .registrarEvento(1L, OrigemSnapshotCarteira.POSITION_CREATED);

        ArgumentCaptor<PortfolioSnapshot> captor = ArgumentCaptor.forClass(PortfolioSnapshot.class);
        verify(snapshots).save(captor.capture());
        assertThat(captor.getValue().getTotalInvestidoBase()).isEqualByComparingTo("0.00000000");
        assertThat(captor.getValue().getDisponibilidadeMonetaria()).isEqualTo(DisponibilidadeMonetaria.AVAILABLE);
    }

    @Test
    void marksSnapshotUnavailableWhenCurrencyOrQuoteIsUnresolved() {
        Carteira carteira = new Carteira(); carteira.setId(1L);
        AtivoCarteira unresolved = position(null, "2", "10", "12");
        CarteiraRepository carteiras = mock(CarteiraRepository.class);
        AtivoCarteiraRepository ativos = mock(AtivoCarteiraRepository.class);
        PortfolioSnapshotRepository snapshots = mock(PortfolioSnapshotRepository.class);
        when(carteiras.getReferenceById(1L)).thenReturn(carteira);
        when(ativos.findByCarteiraId(1L)).thenReturn(List.of(unresolved));
        when(snapshots.findTopByCarteiraIdOrderByDataHoraDesc(1L)).thenReturn(Optional.empty());

        new PortfolioSnapshotService(carteiras, ativos, snapshots)
                .registrarEvento(1L, OrigemSnapshotCarteira.POSITION_UPDATED);

        ArgumentCaptor<PortfolioSnapshot> captor = ArgumentCaptor.forClass(PortfolioSnapshot.class);
        verify(snapshots).save(captor.capture());
        assertThat(captor.getValue().getDisponibilidadeMonetaria()).isEqualTo(DisponibilidadeMonetaria.UNAVAILABLE);
        assertThat(captor.getValue().getTotalInvestidoBase()).isNull();
    }

    private static ContextoMonetario identityContext() {
        return ContextoMonetario.available(Moeda.BRL, Moeda.BRL, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE, java.time.OffsetDateTime.parse("2026-11-15T11:00:00Z"), "IDENTITY");
    }

    private static AtivoCarteira position(Moeda moeda, String quantidade, String preco, String cotacao) {
        Acao acao = new Acao();
        acao.setMoeda(moeda);
        acao.setMercado(moeda == Moeda.BRL ? Mercado.BRASIL : Mercado.EUA);
        acao.setCotacaoAtual(new BigDecimal(cotacao));
        AtivoCarteira position = new AtivoCarteira();
        position.setQuantidade(new BigDecimal(quantidade));
        position.setPrecoMedio(new BigDecimal(preco));
        position.setAcao(acao);
        return position;
    }
}
