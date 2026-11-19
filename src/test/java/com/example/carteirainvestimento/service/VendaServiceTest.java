package com.example.carteirainvestimento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.example.carteirainvestimento.domain.*;
import com.example.carteirainvestimento.dto.venda.VendaRequest;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VendaServiceTest {
    @Mock CarteiraRepository carteiras; @Mock AtivoCarteiraRepository ativos; @Mock VendaRepository vendas;
    private VendaService service; private AtivoCarteira posicao;

    @BeforeEach void setup() {
        service = new VendaService(carteiras, ativos, vendas);
        Carteira carteira = new Carteira(); carteira.setId(1L);
        Acao acao = new Acao(); acao.setId(2L); acao.setTicker("AAPL"); acao.setNomeEmpresa("Apple"); acao.setMoeda(Moeda.USD);
        Corretora corretora = new Corretora(); corretora.setId(3L);
        posicao = new AtivoCarteira(); posicao.setId(4L); posicao.setCarteira(carteira); posicao.setAcao(acao); posicao.setCorretora(corretora);
        posicao.setQuantidade(new BigDecimal("20")); posicao.setPrecoMedio(new BigDecimal("180")); posicao.setDataPrimeiraCompra(LocalDate.of(2025, 1, 1));
        when(carteiras.existsById(1L)).thenReturn(true); when(ativos.findByIdAndCarteiraId(4L, 1L)).thenReturn(Optional.of(posicao));
    }

    @Test void simulationCalculatesProfitAndDoesNotPersist() {
        var result = service.simular(1L, new VendaRequest(4L, new BigDecimal("10"), new BigDecimal("215.30"), new BigDecimal("10.77"), LocalDate.now(), Moeda.USD));
        assertThat(result.valorBruto()).isEqualByComparingTo("2153"); assertThat(result.custoPosicao()).isEqualByComparingTo("1800");
        assertThat(result.resultadoRealizado()).isEqualByComparingTo("342.23"); assertThat(result.quantidadeRestante()).isEqualByComparingTo("10");
        verifyNoInteractions(vendas); verify(ativos, never()).save(any());
    }

    @Test void partialSalePreservesAverageAndReducesPosition() {
        when(vendas.save(any(Venda.class))).thenAnswer(inv -> inv.getArgument(0));
        service.confirmar(1L, new VendaRequest(4L, new BigDecimal("10"), new BigDecimal("200"), BigDecimal.ZERO, LocalDate.now(), Moeda.USD));
        assertThat(posicao.getQuantidade()).isEqualByComparingTo("10"); assertThat(posicao.getPrecoMedio()).isEqualByComparingTo("180"); verify(ativos).save(posicao);
    }

    @Test void rejectsQuantityAboveBalanceAndCurrencyMismatch() {
        assertThatThrownBy(() -> service.simular(1L, new VendaRequest(4L, new BigDecimal("21"), new BigDecimal("200"), BigDecimal.ZERO, LocalDate.now(), Moeda.USD))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.simular(1L, new VendaRequest(4L, BigDecimal.ONE, new BigDecimal("200"), BigDecimal.ZERO, LocalDate.now(), Moeda.BRL))).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(vendas);
    }

    @Test void totalSaleDeletesTheClosedPosition() {
        when(vendas.save(any(Venda.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.confirmar(1L, new VendaRequest(4L, new BigDecimal("20"),
                new BigDecimal("180"), BigDecimal.ZERO, LocalDate.now(), Moeda.USD));

        assertThat(result.quantidadeRestante()).isZero();
        assertThat(result.resultadoRealizado()).isEqualByComparingTo("0");
        verify(ativos).delete(posicao);
        verify(ativos, never()).save(any());
    }

    @Test void calculatesLossAndZeroResultWithoutPersistingSimulation() {
        var loss = service.simular(1L, new VendaRequest(4L, new BigDecimal("5"),
                new BigDecimal("170"), BigDecimal.ZERO, LocalDate.now(), Moeda.USD));
        var zero = service.simular(1L, new VendaRequest(4L, new BigDecimal("5"),
                new BigDecimal("180"), BigDecimal.ZERO, LocalDate.now(), Moeda.USD));

        assertThat(loss.resultadoRealizado()).isEqualByComparingTo("-50");
        assertThat(loss.rentabilidadePercentual()).isEqualByComparingTo("-5.5556");
        assertThat(zero.resultadoRealizado()).isEqualByComparingTo("0");
        assertThat(zero.rentabilidadePercentual()).isEqualByComparingTo("0");
        verifyNoInteractions(vendas);
    }

    @Test void rejectsInvalidSaleDatesPricesFeesAndMissingRequests() {
        assertThatThrownBy(() -> service.simular(1L, null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.simular(1L, new VendaRequest(4L, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ZERO, LocalDate.now().plusDays(1), Moeda.USD)))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.simular(1L, new VendaRequest(4L, BigDecimal.ONE,
                BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now(), Moeda.USD)))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.simular(1L, new VendaRequest(4L, BigDecimal.ONE,
                BigDecimal.ONE, new BigDecimal("-0.01"), LocalDate.now(), Moeda.USD)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test void rejectsMissingWalletAndPositionBeforeCalculating() {
        when(carteiras.existsById(1L)).thenReturn(false);
        assertThatThrownBy(() -> service.simular(1L, new VendaRequest(4L, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ZERO, LocalDate.now(), Moeda.USD)))
                .isInstanceOf(com.example.carteirainvestimento.exception.ResourceNotFoundException.class);

        when(carteiras.existsById(1L)).thenReturn(true);
        when(ativos.findByIdAndCarteiraId(99L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.simular(1L, new VendaRequest(99L, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ZERO, LocalDate.now(), Moeda.USD)))
                .isInstanceOf(com.example.carteirainvestimento.exception.ResourceNotFoundException.class);
    }
}
