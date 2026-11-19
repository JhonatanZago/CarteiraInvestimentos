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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.dto.acao.AcaoCreateRequest;
import com.example.carteirainvestimento.enums.FonteCotacao;
import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.exception.AssetMarketMismatchException;
import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.exception.DuplicateResourceException;
import com.example.carteirainvestimento.exception.ExternalIntegrationException;
import com.example.carteirainvestimento.exception.AssetInUseException;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.integration.dto.CotacaoConsulta;
import com.example.carteirainvestimento.integration.facade.CotacaoFacade;
import com.example.carteirainvestimento.repository.AcaoRepository;
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.repository.HistoricoCotacaoRepository;
import com.example.carteirainvestimento.repository.MovimentacaoFinanceiraRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AcaoRegistrationServiceTest {
    @Mock AcaoRepository acoes;
    @Mock HistoricoCotacaoRepository historicos;
    @Mock CotacaoFacade cotacoes;
    @Mock AtivoCarteiraRepository ativos;
    @Mock MovimentacaoFinanceiraRepository movimentacoes;

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
        when(cotacoes.buscarCotacao("PETR4", Mercado.BRASIL)).thenReturn(quote("PETR4", Mercado.BRASIL, Moeda.BRL, "BR", "B3"));
        assertThatThrownBy(() -> service().registrar(new AcaoCreateRequest("PETR4", Mercado.BRASIL, "BR")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(cotacoes).buscarCotacao("PETR4", Mercado.BRASIL);
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

    @Test
    void rejectsRevalidationForMissingAssetAndPropagatesProviderFailure() {
        when(acoes.findById(70L)).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> service().revalidar(70L))
                .isInstanceOf(ResourceNotFoundException.class);

        Acao existing = new Acao();
        existing.setId(71L); existing.setTicker("PETR4"); existing.setMercado(Mercado.BRASIL);
        when(acoes.findById(71L)).thenReturn(java.util.Optional.of(existing));
        when(cotacoes.buscarCotacao("PETR4", Mercado.BRASIL))
                .thenThrow(new ExternalIntegrationException("provider unavailable"));
        assertThatThrownBy(() -> service().revalidar(71L))
                .isInstanceOf(ExternalIntegrationException.class);
    }

    @Test
    void appliesProviderLogoOrCuratedFallbackDuringRevalidation() {
        Acao withProviderLogo = new Acao();
        withProviderLogo.setId(72L); withProviderLogo.setTicker("PETR4"); withProviderLogo.setMercado(Mercado.BRASIL);
        Acao withoutProviderLogo = new Acao();
        withoutProviderLogo.setId(73L); withoutProviderLogo.setTicker("AAPL"); withoutProviderLogo.setMercado(Mercado.EUA);
        when(acoes.findById(72L)).thenReturn(java.util.Optional.of(withProviderLogo));
        when(acoes.findById(73L)).thenReturn(java.util.Optional.of(withoutProviderLogo));
        when(cotacoes.buscarCotacao("PETR4", Mercado.BRASIL)).thenReturn(new CotacaoConsulta(
                "PETR4", "Petrobras", Mercado.BRASIL, Moeda.BRL, new BigDecimal("40"),
                OffsetDateTime.now(), "https://cdn.example/petr4.svg", FonteCotacao.BRAPI, "BR", "B3", "BVMF"));
        when(cotacoes.buscarCotacao("AAPL", Mercado.EUA)).thenReturn(quote("AAPL", Mercado.EUA, Moeda.USD, "US", "NASDAQ"));
        when(acoes.save(any(Acao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service().revalidar(72L).getLogoUrl()).isEqualTo("https://cdn.example/petr4.svg");
        assertThat(service().revalidar(73L).getLogoUrl()).isEqualTo("/assets/assets/aapl.svg");
    }

    @Test
    void countsAssetsWithoutLogoDuringBatchRevalidation() {
        Acao unknown = new Acao();
        unknown.setId(74L); unknown.setTicker("ZZZ"); unknown.setMercado(Mercado.BRASIL);
        when(acoes.findAll()).thenReturn(java.util.List.of(unknown));
        when(acoes.findById(74L)).thenReturn(java.util.Optional.of(unknown));
        when(cotacoes.buscarCotacao("ZZZ", Mercado.BRASIL)).thenReturn(quote("ZZZ", Mercado.BRASIL, Moeda.BRL, "BR", "B3"));
        when(acoes.save(any(Acao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service().revalidarTodos();

        assertThat(result.atualizados()).isEqualTo(1);
        assertThat(result.semLogo()).isEqualTo(1);
    }

    @Test
    void rejectsTickerWhenBothMarketsCannotFindIt() {
        when(cotacoes.buscarCotacao("MISSING", Mercado.BRASIL))
                .thenThrow(new ResourceNotFoundException("not found"));
        when(cotacoes.buscarCotacao("MISSING", Mercado.EUA))
                .thenThrow(new ResourceNotFoundException("not found"));

        assertThatThrownBy(() -> service().registrar(new AcaoCreateRequest("MISSING", Mercado.BRASIL, "BR")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void distinguishesProviderFailureFromAlternateMarketMismatch() {
        when(cotacoes.buscarCotacao("AAPL", Mercado.BRASIL))
                .thenThrow(new ExternalIntegrationException("provider down"));
        when(cotacoes.buscarCotacao("AAPL", Mercado.EUA))
                .thenReturn(quote("AAPL", Mercado.EUA, Moeda.USD, "US", "NASDAQ"));

        assertThatThrownBy(() -> service().registrar(new AcaoCreateRequest("AAPL", Mercado.BRASIL, "BR")))
                .isInstanceOf(com.example.carteirainvestimento.exception.AssetCountryMismatchException.class);
    }

    @Test
    void propagatesProviderFailureWhenAlternateMarketAlsoFails() {
        when(cotacoes.buscarCotacao("AAPL", Mercado.BRASIL))
                .thenThrow(new ExternalIntegrationException("provider down"));
        when(cotacoes.buscarCotacao("AAPL", Mercado.EUA))
                .thenThrow(new ExternalIntegrationException("provider down"));

        assertThatThrownBy(() -> service().registrar(new AcaoCreateRequest("AAPL", Mercado.BRASIL, "BR")))
                .isInstanceOf(ExternalIntegrationException.class);
    }

    @Test
    void rejectsProviderTickerMismatchAndNullResponse() {
        when(cotacoes.buscarCotacao("PETR4", Mercado.BRASIL))
                .thenReturn(quote("VALE3", Mercado.BRASIL, Moeda.BRL, "BR", "B3"));
        assertThatThrownBy(() -> service().registrar(new AcaoCreateRequest("PETR4", Mercado.BRASIL, "BR")))
                .isInstanceOf(BusinessRuleException.class);

        when(cotacoes.buscarCotacao("ITUB4", Mercado.BRASIL)).thenReturn(null);
        assertThatThrownBy(() -> service().registrar(new AcaoCreateRequest("ITUB4", Mercado.BRASIL, "BR")))
                .isInstanceOf(ExternalIntegrationException.class);
    }

    @Test
    void rejectsDuplicateHistoricalQuotationAfterSavingAsset() {
        when(acoes.existsByTicker("PETR4")).thenReturn(false);
        when(acoes.save(any(Acao.class))).thenAnswer(invocation -> {
            Acao saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });
        when(historicos.existsByAcaoIdAndDataHoraCotacao(any(), any())).thenReturn(true);
        when(cotacoes.buscarCotacao("PETR4", Mercado.BRASIL))
                .thenReturn(quote("PETR4", Mercado.BRASIL, Moeda.BRL, "BR", "B3"));

        assertThatThrownBy(() -> service().registrar(new AcaoCreateRequest("PETR4", Mercado.BRASIL, "BR")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deletesUnusedAssetAndItsTechnicalHistory() {
        Acao existing = new Acao();
        existing.setId(44L);
        when(acoes.findById(44L)).thenReturn(java.util.Optional.of(existing));
        when(ativos.findCarteiraIdsByAcaoId(44L)).thenReturn(java.util.List.of());

        service().excluir(44L);

        verify(historicos).deleteByAcaoId(44L);
        verify(acoes).delete(existing);
    }

    @Test
    void rejectsDeletingAssetWithFinancialMovement() {
        Acao existing = new Acao();
        existing.setId(45L);
        when(acoes.findById(45L)).thenReturn(java.util.Optional.of(existing));
        when(ativos.findCarteiraIdsByAcaoId(45L)).thenReturn(java.util.List.of());
        when(movimentacoes.existsByAcaoId(45L)).thenReturn(true);

        assertThatThrownBy(() -> serviceWithMovements().excluir(45L))
                .isInstanceOf(AssetInUseException.class);
        verify(acoes, never()).delete(any());
    }

    @Test
    void revalidationRejectsIncompleteProviderMetadata() {
        Acao existing = new Acao();
        existing.setId(46L);
        existing.setTicker("PETR4");
        existing.setMercado(Mercado.BRASIL);
        when(acoes.findById(46L)).thenReturn(java.util.Optional.of(existing));
        when(cotacoes.buscarCotacao("PETR4", Mercado.BRASIL)).thenReturn(new CotacaoConsulta(
                "PETR4", "Petrobras", Mercado.BRASIL, null, new BigDecimal("10"),
                OffsetDateTime.now(), null, FonteCotacao.BRAPI, "BR", "B3", "BVMF"));

        assertThatThrownBy(() -> service().revalidar(46L)).isInstanceOf(com.example.carteirainvestimento.exception.BusinessRuleException.class);
        verify(acoes, never()).save(any());
    }

    @Test
    void explainsCountryMismatchAfterTheSelectedMarketCannotFindTicker() {
        when(cotacoes.buscarCotacao("AAPL", Mercado.BRASIL))
                .thenThrow(new ResourceNotFoundException("not found"));
        when(cotacoes.buscarCotacao("AAPL", Mercado.EUA))
                .thenReturn(quote("AAPL", Mercado.EUA, Moeda.USD, "US", "NASDAQ"));

        assertThatThrownBy(() -> service().registrar(new AcaoCreateRequest("AAPL", Mercado.BRASIL, "BR")))
                .isInstanceOf(AssetMarketMismatchException.class);
    }

    @Test
    void revalidatesAllAssetsAndReportsIndividualFailures() {
        Acao valid = new Acao();
        valid.setId(51L); valid.setTicker("PETR4"); valid.setMercado(Mercado.BRASIL); valid.setMoeda(Moeda.BRL);
        Acao invalid = new Acao();
        invalid.setId(52L); invalid.setTicker("AAPL"); invalid.setMercado(Mercado.EUA); invalid.setMoeda(Moeda.USD);
        when(acoes.findAll()).thenReturn(java.util.List.of(valid, invalid));
        when(acoes.findById(51L)).thenReturn(java.util.Optional.of(valid));
        when(acoes.findById(52L)).thenReturn(java.util.Optional.of(invalid));
        when(cotacoes.buscarCotacao("PETR4", Mercado.BRASIL))
                .thenReturn(quote("PETR4", Mercado.BRASIL, Moeda.BRL, "BR", "B3"));
        when(cotacoes.buscarCotacao("AAPL", Mercado.EUA))
                .thenThrow(new ExternalIntegrationException("provider unavailable"));
        when(acoes.save(any(Acao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service().revalidarTodos();

        assertThat(result.processados()).isEqualTo(2);
        assertThat(result.atualizados()).isEqualTo(1);
        assertThat(result.erros()).hasSize(1);
        assertThat(result.erros().get(0)).contains("AAPL");
        assertThat(result.detalhes()).hasSize(2);
        verify(acoes).save(valid);
    }

    private AcaoRegistrationService service() { return new AcaoRegistrationService(acoes, historicos, cotacoes, ativos); }

    private AcaoRegistrationService serviceWithMovements() {
        return new AcaoRegistrationService(acoes, historicos, cotacoes, ativos, movimentacoes);
    }

    private CotacaoConsulta quote(String ticker, Mercado mercado, Moeda moeda, String country, String exchange) {
        return new CotacaoConsulta(ticker, ticker + " Inc.", mercado, moeda, new BigDecimal("100.00"),
                OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC), null, FonteCotacao.BRAPI,
                country, exchange, exchange.equals("NASDAQ") ? "XNAS" : "BVMF");
    }
}
