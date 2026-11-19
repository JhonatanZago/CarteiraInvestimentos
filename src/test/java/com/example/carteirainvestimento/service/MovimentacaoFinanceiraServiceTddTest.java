package com.example.carteirainvestimento.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.domain.Carteira;
import com.example.carteirainvestimento.domain.Corretora;
import com.example.carteirainvestimento.domain.MovimentacaoFinanceira;
import com.example.carteirainvestimento.dto.movimentacao.MovimentacaoFinanceiraRequest;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.enums.TipoMovimentacao;
import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.repository.AcaoRepository;
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.repository.CarteiraRepository;
import com.example.carteirainvestimento.repository.CorretoraRepository;
import com.example.carteirainvestimento.repository.MovimentacaoFinanceiraRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MovimentacaoFinanceiraServiceTddTest {
    private final MovimentacaoFinanceiraRepository movimentacoes = mock(MovimentacaoFinanceiraRepository.class);
    private final CarteiraRepository carteiras = mock(CarteiraRepository.class);
    private final AcaoRepository acoes = mock(AcaoRepository.class);
    private final CorretoraRepository corretoras = mock(CorretoraRepository.class);
    private final ExchangeRateService exchangeRates = mock(ExchangeRateService.class);
    private final MovimentacaoFinanceiraService service = new MovimentacaoFinanceiraService(
            movimentacoes, carteiras, acoes, corretoras, exchangeRates);

    @Test
    void rejectsExplicitlyInvalidExchangeRateInsteadOfSilentlyReplacingIt() {
        Carteira carteira = new Carteira();
        Acao acao = new Acao();
        acao.setMoeda(Moeda.USD);
        Corretora corretora = new Corretora();
        when(carteiras.findById(1L)).thenReturn(java.util.Optional.of(carteira));
        when(acoes.findById(2L)).thenReturn(java.util.Optional.of(acao));
        when(corretoras.findById(3L)).thenReturn(java.util.Optional.of(corretora));
        when(exchangeRates.usdToBrl()).thenReturn(new BigDecimal("5.00"));

        MovimentacaoFinanceiraRequest request = new MovimentacaoFinanceiraRequest(
                TipoMovimentacao.COMPRA, BigDecimal.ONE, new BigDecimal("10.00"),
                new BigDecimal("-1.00"), null, "manual", BigDecimal.ZERO, LocalDate.now());

        assertThrows(BusinessRuleException.class, () -> service.registrar(1L, 2L, 3L, request));
        verify(exchangeRates, never()).usdToBrl();
        verify(movimentacoes, never()).save(any());
    }

    @Test
    void rejectsSaleAboveCurrentPosition() {
        Carteira carteira = new Carteira();
        Acao acao = new Acao();
        acao.setMoeda(Moeda.BRL);
        Corretora corretora = new Corretora();
        MovimentacaoFinanceira purchase = new MovimentacaoFinanceira();
        purchase.setTipo(TipoMovimentacao.COMPRA);
        purchase.setQuantidade(BigDecimal.ONE);
        when(carteiras.findById(1L)).thenReturn(java.util.Optional.of(carteira));
        when(acoes.findById(2L)).thenReturn(java.util.Optional.of(acao));
        when(corretoras.findById(3L)).thenReturn(java.util.Optional.of(corretora));
        when(movimentacoes.findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscIdAsc(1L, 2L))
                .thenReturn(List.of(purchase));

        MovimentacaoFinanceiraRequest request = new MovimentacaoFinanceiraRequest(
                TipoMovimentacao.VENDA, new BigDecimal("1.01"), new BigDecimal("10.00"),
                null, null, null, BigDecimal.ZERO, LocalDate.now());

        assertThrows(BusinessRuleException.class, () -> service.registrar(1L, 2L, 3L, request));
        verify(movimentacoes, never()).save(any());
    }

    @Test
    void recordsUsdOperationWithProviderRateWhenRateIsAbsent() {
        Carteira carteira = new Carteira();
        Acao acao = new Acao();
        acao.setMoeda(Moeda.USD);
        Corretora corretora = new Corretora();
        when(carteiras.findById(1L)).thenReturn(java.util.Optional.of(carteira));
        when(acoes.findById(2L)).thenReturn(java.util.Optional.of(acao));
        when(corretoras.findById(3L)).thenReturn(java.util.Optional.of(corretora));
        when(exchangeRates.usdToBrl()).thenReturn(new BigDecimal("5.00"));
        when(movimentacoes.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MovimentacaoFinanceiraRequest request = new MovimentacaoFinanceiraRequest(
                TipoMovimentacao.COMPRA, new BigDecimal("2"), new BigDecimal("10.00"),
                null, null, null, BigDecimal.ZERO, LocalDate.now());

        MovimentacaoFinanceira saved = service.registrar(1L, 2L, 3L, request);

        assertEquals(Moeda.USD, saved.getMoeda());
        assertEquals(0, new BigDecimal("20.00").compareTo(saved.getValorOriginal()));
        assertEquals(0, new BigDecimal("5.00").compareTo(saved.getCambioParaBrl()));
        assertEquals(0, new BigDecimal("100.00").compareTo(saved.getValorBrl()));
    }

    @Test
    void rejectsUsdOperationWhenProviderRateIsUnavailable() {
        Carteira carteira = new Carteira();
        Acao acao = new Acao();
        acao.setMoeda(Moeda.USD);
        Corretora corretora = new Corretora();
        when(carteiras.findById(1L)).thenReturn(java.util.Optional.of(carteira));
        when(acoes.findById(2L)).thenReturn(java.util.Optional.of(acao));
        when(corretoras.findById(3L)).thenReturn(java.util.Optional.of(corretora));
        when(exchangeRates.usdToBrl()).thenReturn(null);

        MovimentacaoFinanceiraRequest request = new MovimentacaoFinanceiraRequest(
                TipoMovimentacao.COMPRA, BigDecimal.ONE, new BigDecimal("10.00"),
                null, null, null, BigDecimal.ZERO, LocalDate.now());

        assertThrows(BusinessRuleException.class, () -> service.registrar(1L, 2L, 3L, request));
        verify(movimentacoes, never()).save(any());
    }

    @Test
    void calculatesBalanceAndRealizedResultAcrossPartialSale() {
        MovimentacaoFinanceira compra = movimento(TipoMovimentacao.COMPRA, "10", "100", "1000", null);
        MovimentacaoFinanceira venda = movimento(TipoMovimentacao.VENDA, "4", "140", "560", BigDecimal.ZERO);
        when(movimentacoes.findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscIdAsc(1L, 2L))
                .thenReturn(List.of(compra, venda));

        assertEquals(0, new BigDecimal("6").compareTo(service.saldo(1L, 2L)));
        assertEquals(0, new BigDecimal("160").compareTo(service.resultadoRealizado(1L, 2L)));
    }

    @Test
    void ignoresSaleBeforePurchaseAndTreatsMissingCostsAsZero() {
        MovimentacaoFinanceira venda = movimento(TipoMovimentacao.VENDA, "2", "50", "100", null);
        MovimentacaoFinanceira compra = movimento(TipoMovimentacao.COMPRA, "2", "50", "100", null);
        when(movimentacoes.findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscIdAsc(1L, 2L))
                .thenReturn(List.of(venda, compra));

        assertEquals(0, BigDecimal.ZERO.compareTo(service.resultadoRealizado(1L, 2L)));
    }

    @Test
    void rejectsEveryInvalidMovementInputBeforeRepositoryAccess() {
        assertThrows(BusinessRuleException.class, () -> service.registrar(1L, 2L, 3L, null));
        assertThrows(BusinessRuleException.class, () -> service.registrar(1L, 2L, 3L,
                request(null, BigDecimal.ONE, BigDecimal.ONE, LocalDate.now())));
        assertThrows(BusinessRuleException.class, () -> service.registrar(1L, 2L, 3L,
                request(TipoMovimentacao.COMPRA, null, BigDecimal.ONE, LocalDate.now())));
        assertThrows(BusinessRuleException.class, () -> service.registrar(1L, 2L, 3L,
                request(TipoMovimentacao.COMPRA, new BigDecimal("-1"), BigDecimal.ONE, LocalDate.now())));
        assertThrows(BusinessRuleException.class, () -> service.registrar(1L, 2L, 3L,
                request(TipoMovimentacao.COMPRA, BigDecimal.ONE, null, LocalDate.now())));
        assertThrows(BusinessRuleException.class, () -> service.registrar(1L, 2L, 3L,
                request(TipoMovimentacao.COMPRA, BigDecimal.ONE, new BigDecimal("-1"), LocalDate.now())));
        assertThrows(BusinessRuleException.class, () -> service.registrar(1L, 2L, 3L,
                request(TipoMovimentacao.COMPRA, BigDecimal.ONE, BigDecimal.ONE, null)));
        assertThrows(BusinessRuleException.class, () -> service.registrar(1L, 2L, 3L,
                request(TipoMovimentacao.COMPRA, BigDecimal.ONE, BigDecimal.ONE, LocalDate.now().plusDays(1))));
        verifyNoInteractions(carteiras, acoes, corretoras, movimentacoes);
    }

    private MovimentacaoFinanceiraRequest request(TipoMovimentacao tipo, BigDecimal quantidade,
            BigDecimal preco, LocalDate data) {
        return new MovimentacaoFinanceiraRequest(tipo, quantidade, preco, null, null, null,
                BigDecimal.ZERO, data);
    }

    private MovimentacaoFinanceira movimento(TipoMovimentacao tipo, String quantidade,
            String preco, String valorOriginal, BigDecimal custos) {
        MovimentacaoFinanceira item = new MovimentacaoFinanceira();
        item.setTipo(tipo); item.setQuantidade(new BigDecimal(quantidade));
        item.setPrecoUnitario(new BigDecimal(preco));
        item.setValorOriginal(new BigDecimal(valorOriginal));
        item.setCustosBrl(custos);
        return item;
    }
}
