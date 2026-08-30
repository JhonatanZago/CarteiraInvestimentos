package com.example.carteirainvestimento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.domain.AtivoCarteira;
import com.example.carteirainvestimento.domain.Carteira;
import com.example.carteirainvestimento.domain.Corretora;
import com.example.carteirainvestimento.dto.carteira.AtivoCarteiraRequest;
import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.exception.DuplicateResourceException;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.repository.AcaoRepository;
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.repository.CarteiraRepository;
import com.example.carteirainvestimento.repository.CorretoraRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AtivoCarteiraServiceTest {

    @Mock private CarteiraRepository carteiras;
    @Mock private AcaoRepository acoes;
    @Mock private CorretoraRepository corretoras;
    @Mock private AtivoCarteiraRepository ativos;

    private AtivoCarteiraService service;
    private Carteira carteira;
    private Acao acao;
    private Corretora corretora;

    @BeforeEach
    void setUp() {
        service = new AtivoCarteiraService(carteiras, acoes, corretoras, ativos);
        carteira = new Carteira(); carteira.setId(1L);
        acao = new Acao(); acao.setId(2L);
        corretora = new Corretora(); corretora.setId(3L);
    }

    @Test
    void criaPosicaoComDadosValidos() {
        AtivoCarteiraRequest request = request(new BigDecimal("10"), new BigDecimal("25.50"), LocalDate.now());
        when(carteiras.findById(1L)).thenReturn(Optional.of(carteira));
        when(ativos.existsByCarteiraIdAndAcaoIdAndCorretoraId(1L, 2L, 3L)).thenReturn(false);
        when(acoes.findById(2L)).thenReturn(Optional.of(acao));
        when(corretoras.findById(3L)).thenReturn(Optional.of(corretora));
        when(ativos.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AtivoCarteira result = service.criar(1L, request);

        assertThat(result.getCarteira()).isSameAs(carteira);
        assertThat(result.getAcao()).isSameAs(acao);
        assertThat(result.getCorretora()).isSameAs(corretora);
        assertThat(result.getQuantidade()).isEqualByComparingTo("10");
        verify(ativos).save(any(AtivoCarteira.class));
    }

    @Test
    void rejeitaQuantidadePrecoOuDataInvalidos() {
        assertThatThrownBy(() -> service.criar(1L, request(BigDecimal.ZERO, BigDecimal.ONE, LocalDate.now())))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.criar(1L, request(BigDecimal.ONE, BigDecimal.ZERO, LocalDate.now())))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.criar(1L, request(BigDecimal.ONE, BigDecimal.ONE, LocalDate.now().plusDays(1))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejeitaPosicaoDuplicada() {
        when(carteiras.findById(1L)).thenReturn(Optional.of(carteira));
        when(ativos.existsByCarteiraIdAndAcaoIdAndCorretoraId(1L, 2L, 3L)).thenReturn(true);

        assertThatThrownBy(() -> service.criar(1L, request(BigDecimal.ONE, BigDecimal.ONE, LocalDate.now())))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void rejeitaAcaoOuCorretoraInexistente() {
        when(carteiras.findById(1L)).thenReturn(Optional.of(carteira));
        when(ativos.existsByCarteiraIdAndAcaoIdAndCorretoraId(1L, 2L, 3L)).thenReturn(false);
        when(acoes.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(1L, request(BigDecimal.ONE, BigDecimal.ONE, LocalDate.now())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void atualizaSemPermitirOutraCombinacaoIgual() {
        AtivoCarteira ativo = new AtivoCarteira();
        ativo.setId(10L); ativo.setCarteira(carteira);
        when(ativos.findById(10L)).thenReturn(Optional.of(ativo));
        when(ativos.existsByCarteiraIdAndAcaoIdAndCorretoraIdAndIdNot(1L, 2L, 3L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, 10L, request(BigDecimal.ONE, BigDecimal.ONE, LocalDate.now())))
                .isInstanceOf(DuplicateResourceException.class);
    }

    private AtivoCarteiraRequest request(BigDecimal quantidade, BigDecimal precoMedio, LocalDate data) {
        return new AtivoCarteiraRequest(2L, 3L, quantidade, precoMedio, data);
    }
}
