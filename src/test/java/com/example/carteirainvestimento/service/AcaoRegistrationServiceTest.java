package com.example.carteirainvestimento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.dto.acao.AcaoCreateRequest;
import com.example.carteirainvestimento.enums.FonteCotacao;
import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.exception.AssetMarketMismatchException;
import com.example.carteirainvestimento.exception.DuplicateResourceException;
import com.example.carteirainvestimento.exception.ExternalIntegrationException;
import com.example.carteirainvestimento.integration.dto.CotacaoConsulta;
import com.example.carteirainvestimento.integration.facade.CotacaoFacade;
import com.example.carteirainvestimento.repository.AcaoRepository;
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.repository.HistoricoCotacaoRepository;

@ExtendWith(MockitoExtension.class)
class AcaoRegistrationServiceTest {
    @Mock AcaoRepository acoes;
    @Mock HistoricoCotacaoRepository historicos;
    @Mock CotacaoFacade cotacoes;
    @Mock AtivoCarteiraRepository ativos;

    @Test
    void acceptsBrazilianListingOnlyWhenCountryAndCurrencyMatch() {
        when(acoes.save(any(Acao.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(acoes.existsByTicker("PETR4")).thenReturn(false);
        when(historicos.existsByAcaoIdAndDataHoraCotacao(any(), any())).thenReturn(false);
        when(cotacoes.buscarCotacao("PETR4", Mercado.BRASIL)).thenReturn(quote("PETR4", Mercado.BRASIL, Moeda.BRL, "BR", "B3"));

        Acao result = service().registrar(new AcaoCreateRequest(" petr4 ", Mercado.BRASIL, "br"));

        assertThat(result.getMoeda()).isEqualTo(Moeda.BRL);
        assertThat(result.getListingCountryCode()).isEqualTo("BR");
        verify(acoes).save(any(Acao.class));
    }

    @Test
    void rejectsAaplSelectedAsBrazil() {
        when(acoes.existsByTicker("AAPL")).thenReturn(false);
        when(cotacoes.buscarCotacao("AAPL", Mercado.BRASIL)).thenReturn(quote("AAPL", Mercado.EUA, Moeda.USD, "US", "NASDAQ"));

        assertThatThrownBy(() -> service().registrar(new AcaoCreateRequest("AAPL", Mercado.BRASIL, "BR")))
                .isInstanceOf(AssetMarketMismatchException.class);
        verify(acoes, never()).save(any());
    }

    @Test
    void acceptsAaplSelectedAsUnitedStates() {
        when(acoes.save(any(Acao.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(acoes.existsByTicker("AAPL")).thenReturn(false);
        when(historicos.existsByAcaoIdAndDataHoraCotacao(any(), any())).thenReturn(false);
        when(cotacoes.buscarCotacao("AAPL", Mercado.EUA)).thenReturn(quote("AAPL", Mercado.EUA, Moeda.USD, "US", "NASDAQ"));

        assertThat(service().registrar(new AcaoCreateRequest("AAPL", Mercado.EUA, "US")).getMoeda()).isEqualTo(Moeda.USD);
    }

    @Test
    void rejectsMissingCountryBeforeProviderCall() {
        assertThatThrownBy(() -> service().registrar(new AcaoCreateRequest("PETR4", Mercado.BRASIL, "")))
                .isInstanceOf(com.example.carteirainvestimento.exception.BusinessRuleException.class);
        verifyNoInteractions(cotacoes);
    }

    @Test
    void rejectsDuplicateBeforeCallingProvider() {
        when(acoes.existsByTicker("PETR4")).thenReturn(true);
        assertThatThrownBy(() -> service().registrar(new AcaoCreateRequest("PETR4", Mercado.BRASIL, "BR")))
                .isInstanceOf(DuplicateResourceException.class);
        verifyNoInteractions(cotacoes);
    }

    @Test
    void rejectsIncompleteProviderResponseWithoutPersisting() {
        when(acoes.existsByTicker("PETR4")).thenReturn(false);
        when(cotacoes.buscarCotacao("PETR4", Mercado.BRASIL)).thenReturn(new CotacaoConsulta(
                "PETR4", "Petrobras", Mercado.BRASIL, null, new BigDecimal("10"),
                OffsetDateTime.now(), null, FonteCotacao.BRAPI, "BR", "B3", "BVMF"));
        assertThatThrownBy(() -> service().registrar(new AcaoCreateRequest("PETR4", Mercado.BRASIL, "BR")))
                .isInstanceOf(com.example.carteirainvestimento.exception.BusinessRuleException.class);
        verify(acoes, never()).save(any());
    }

    @Test
    void propagatesProviderFailureWithoutPersisting() {
        when(acoes.existsByTicker("PETR4")).thenReturn(false);
        when(cotacoes.buscarCotacao("PETR4", Mercado.BRASIL)).thenThrow(new ExternalIntegrationException("provider unavailable"));
        assertThatThrownBy(() -> service().registrar(new AcaoCreateRequest("PETR4", Mercado.BRASIL, "BR")))
                .isInstanceOf(ExternalIntegrationException.class);
        verify(acoes, never()).save(any());
    }

    @Test
    void revalidatesInPlaceAndPreservesTheAssetIdentity() {
        Acao existing = new Acao();
        existing.setId(77L); existing.setTicker("AAPL"); existing.setMercado(Mercado.EUA);
        existing.setMoeda(Moeda.USD); existing.setLogoUrl("https://old.example/logo.png");
        when(acoes.findById(77L)).thenReturn(java.util.Optional.of(existing));
        when(cotacoes.buscarCotacao("AAPL", Mercado.EUA)).thenReturn(quote("AAPL", Mercado.EUA, Moeda.USD, "US", "NASDAQ"));
        when(acoes.save(any(Acao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Acao result = service().revalidar(77L);

        assertThat(result.getId()).isEqualTo(77L);
        assertThat(result.getTicker()).isEqualTo("AAPL");
        assertThat(result.getMoeda()).isEqualTo(Moeda.USD);
        verify(acoes).save(existing);
    }

    private AcaoRegistrationService service() { return new AcaoRegistrationService(acoes, historicos, cotacoes, ativos); }

    private CotacaoConsulta quote(String ticker, Mercado mercado, Moeda moeda, String country, String exchange) {
        return new CotacaoConsulta(ticker, ticker + " Inc.", mercado, moeda, new BigDecimal("100.00"),
                OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC), null, FonteCotacao.BRAPI,
                country, exchange, exchange.equals("NASDAQ") ? "XNAS" : "BVMF");
    }
}
