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
import com.example.carteirainvestimento.dto.insight.ClassificacaoAlocacao;
import com.example.carteirainvestimento.enums.Mercado;
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
    @Mock private PortfolioSnapshotService snapshots;

    private AtivoCarteiraService service;
    private Carteira carteira;
    private Acao acao;
    private Corretora corretora;

    @BeforeEach
    void setUp() {
        service = new AtivoCarteiraService(carteiras, acoes, corretoras, ativos, snapshots);
        carteira = new Carteira(); carteira.setId(1L);
        acao = new Acao(); acao.setId(2L);
        corretora = new Corretora(); corretora.setId(3L);
    }

    @Test
    void criaPosicaoComDadosValidos() {
        AtivoCarteiraRequest request = request(new BigDecimal("10"), new BigDecimal("25.50"), LocalDate.now());
        when(carteiras.findById(1L)).thenReturn(Optional.of(carteira));
        when(ativos.findByCarteiraIdAndAcaoIdAndCorretoraId(1L, 2L, 3L)).thenReturn(Optional.empty());
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
    void somaCompraNaPosicaoExistente() {
        when(carteiras.findById(1L)).thenReturn(Optional.of(carteira));
        AtivoCarteira existente = new AtivoCarteira(); existente.setCarteira(carteira); existente.setQuantidade(new BigDecimal("5")); existente.setPrecoMedio(new BigDecimal("100"));
        when(ativos.findByCarteiraIdAndAcaoIdAndCorretoraId(1L, 2L, 3L)).thenReturn(Optional.of(existente));
        when(ativos.save(existente)).thenReturn(existente);

        AtivoCarteira result = service.criar(1L, request(new BigDecimal("5"), new BigDecimal("70"), LocalDate.now()));
        assertThat(result.getQuantidade()).isEqualByComparingTo("10");
        assertThat(result.getPrecoMedio()).isEqualByComparingTo("85");
    }

    @Test
    void rejeitaAcaoOuCorretoraInexistente() {
        when(carteiras.findById(1L)).thenReturn(Optional.of(carteira));
        when(ativos.findByCarteiraIdAndAcaoIdAndCorretoraId(1L, 2L, 3L)).thenReturn(Optional.empty());
        when(acoes.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(1L, request(BigDecimal.ONE, BigDecimal.ONE, LocalDate.now())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void atualizaSemPermitirOutraCombinacaoIgual() {
        AtivoCarteira ativo = new AtivoCarteira();
        ativo.setId(10L); ativo.setCarteira(carteira);
        when(carteiras.findById(1L)).thenReturn(Optional.of(carteira));
        when(ativos.findById(10L)).thenReturn(Optional.of(ativo));
        when(ativos.existsByCarteiraIdAndAcaoIdAndCorretoraIdAndIdNot(1L, 2L, 3L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, 10L, request(BigDecimal.ONE, BigDecimal.ONE, LocalDate.now())))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void atualizaEExcluiPosicaoPertencenteACarteira() {
        AtivoCarteira ativo = new AtivoCarteira();
        ativo.setId(10L); ativo.setCarteira(carteira);
        when(carteiras.findById(1L)).thenReturn(Optional.of(carteira));
        when(ativos.findById(10L)).thenReturn(Optional.of(ativo));
        when(ativos.existsByCarteiraIdAndAcaoIdAndCorretoraIdAndIdNot(1L, 2L, 3L, 10L)).thenReturn(false);
        when(acoes.findById(2L)).thenReturn(Optional.of(acao));
        when(corretoras.findById(3L)).thenReturn(Optional.of(corretora));
        when(ativos.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.atualizar(1L, 10L, request(BigDecimal.ONE, BigDecimal.ONE, LocalDate.now()));
        service.excluir(1L, 10L);

        verify(ativos).save(ativo);
        verify(ativos).delete(ativo);
        verify(ativos).flush();
    }

    @Test
    void rejeitaCarteiraInexistenteEPosicaoDeOutraCarteira() {
        when(carteiras.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.listar(1L))
                .isInstanceOf(ResourceNotFoundException.class);

        Carteira outra = new Carteira(); outra.setId(2L);
        AtivoCarteira ativo = new AtivoCarteira(); ativo.setId(10L); ativo.setCarteira(outra);
        when(carteiras.findById(1L)).thenReturn(Optional.of(carteira));
        when(ativos.findById(10L)).thenReturn(Optional.of(ativo));
        assertThatThrownBy(() -> service.excluir(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listaComFiltrosEOrdenacoesFinanceiras() {
        acao.setTicker("PETR4"); acao.setMercado(Mercado.BRASIL); acao.setCotacaoAtual(new BigDecimal("30"));
        Acao outraAcao = new Acao(); outraAcao.setId(5L); outraAcao.setTicker("AAPL");
        outraAcao.setMercado(Mercado.EUA); outraAcao.setCotacaoAtual(new BigDecimal("200"));
        AtivoCarteira br = posicao(acao, "2", "10");
        AtivoCarteira us = posicao(outraAcao, "1", "100");
        when(carteiras.findById(1L)).thenReturn(Optional.of(carteira));
        when(ativos.findByCarteiraId(1L)).thenReturn(java.util.List.of(br, us));

        assertThat(service.listar(1L, Mercado.BRASIL, null, "pet", "valorAtual")).containsExactly(br);
        assertThat(service.listar(1L, null, ClassificacaoAlocacao.ACOES_BRASIL, null, "resultado")).containsExactly(br);
        assertThat(service.listar(1L, null, null, "", "rentabilidade")).hasSize(2);
        assertThat(service.listar(1L, null, null, null, null)).hasSize(2);
    }

    private AtivoCarteira posicao(Acao ativo, String quantidade, String preco) {
        AtivoCarteira posicao = new AtivoCarteira();
        posicao.setCarteira(carteira); posicao.setAcao(ativo); posicao.setCorretora(corretora);
        posicao.setQuantidade(new BigDecimal(quantidade)); posicao.setPrecoMedio(new BigDecimal(preco));
        posicao.setDataPrimeiraCompra(LocalDate.now());
        return posicao;
    }

    private AtivoCarteiraRequest request(BigDecimal quantidade, BigDecimal precoMedio, LocalDate data) {
        return new AtivoCarteiraRequest(2L, 3L, quantidade, precoMedio, data);
    }
}
