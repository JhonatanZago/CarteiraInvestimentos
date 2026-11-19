package com.example.carteirainvestimento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.domain.AtivoCarteira;
import com.example.carteirainvestimento.dto.dashboard.DashboardCarteiraResponse;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.dto.financeiro.ContextoMonetario;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.enums.TipoMovimentacao;
import com.example.carteirainvestimento.domain.MovimentacaoFinanceira;
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.repository.CarteiraRepository;
import com.example.carteirainvestimento.repository.MovimentacaoFinanceiraRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardCarteiraServiceTest {

    @Mock private CarteiraRepository carteiras;
    @Mock private AtivoCarteiraRepository ativos;
    @Mock private ExchangeRateService exchangeRates;

    private DashboardCarteiraService service;

    @BeforeEach
    void setUp() {
        service = new DashboardCarteiraService(carteiras, ativos);
    }

    @Test
    void calculaIndicadoresComBaseNasUltimasCotacoesArmazenadas() {
        OffsetDateTime primeiraAtualizacao = OffsetDateTime.of(2026, 8, 25, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime ultimaAtualizacao = OffsetDateTime.of(2026, 8, 26, 15, 30, 0, 0, ZoneOffset.UTC);
        when(carteiras.existsById(1L)).thenReturn(true);
        when(ativos.findByCarteiraId(1L)).thenReturn(List.of(
                posicao("10", "10", "12", primeiraAtualizacao),
                posicao("5", "20", "30", ultimaAtualizacao)));

        DashboardCarteiraResponse dashboard = service.calcular(1L);

        assertThat(dashboard.valorInvestido()).isEqualByComparingTo("200.00");
        assertThat(dashboard.valorAtual()).isEqualByComparingTo("270.00");
        assertThat(dashboard.resultado()).isEqualByComparingTo("70.00");
        assertThat(dashboard.rentabilidadePercentual()).isEqualByComparingTo("35.0000");
        assertThat(dashboard.ultimaAtualizacao()).isEqualTo(ultimaAtualizacao);
        assertThat(dashboard.quantidadeAtivos()).isEqualTo(2);
        assertThat(dashboard.composicao()).hasSize(2);
        assertThat(dashboard.composicao().get(0).valorInvestido()).isEqualByComparingTo("100.00");
        assertThat(dashboard.composicao().get(1).resultado()).isEqualByComparingTo("50.00");
    }

    @Test
    void retornaZeroSemDivisaoPorZeroQuandoNaoHaInvestimento() {
        when(carteiras.existsById(1L)).thenReturn(true);
        when(ativos.findByCarteiraId(1L)).thenReturn(List.of());

        DashboardCarteiraResponse dashboard = service.calcular(1L);

        assertThat(dashboard.valorInvestido()).isEqualByComparingTo("0.00");
        assertThat(dashboard.valorAtual()).isEqualByComparingTo("0.00");
        assertThat(dashboard.resultado()).isEqualByComparingTo("0.00");
        assertThat(dashboard.rentabilidadePercentual()).isEqualByComparingTo("0");
        assertThat(dashboard.ultimaAtualizacao()).isNull();
        assertThat(dashboard.quantidadeAtivos()).isZero();
        assertThat(dashboard.composicao()).isEmpty();
    }

    @Test
    void calculaResultadoNegativoEMarcaTotaisComoIndisponiveisQuandoFaltaCotacao() {
        when(carteiras.existsById(1L)).thenReturn(true);
        when(ativos.findByCarteiraId(1L)).thenReturn(List.of(
                posicao("2", "20", "15", OffsetDateTime.now(ZoneOffset.UTC)),
                posicao("0", "20", "15", OffsetDateTime.now(ZoneOffset.UTC)),
                posicao("2", "20", null, null)));

        DashboardCarteiraResponse dashboard = service.calcular(1L);

        assertThat(dashboard.valorInvestido()).isEqualByComparingTo("80.00");
        assertThat(dashboard.valorAtual()).isNull();
        assertThat(dashboard.resultado()).isNull();
        assertThat(dashboard.rentabilidadePercentual()).isNull();
        assertThat(dashboard.composicao().get(2).valorAtual()).isNull();
        assertThat(dashboard.composicao().get(2).rentabilidadePercentual()).isNull();
    }

    @Test
    void converteCadaComponenteUsdAntesDeConsolidarCarteiraMista() {
        when(carteiras.existsById(1L)).thenReturn(true);
        when(exchangeRates.usdToBrlContext()).thenReturn(ContextoMonetario.available(
                Moeda.USD, Moeda.BRL, BigDecimal.ONE, new BigDecimal("5"), new BigDecimal("5"),
                OffsetDateTime.parse("2026-11-15T11:00:00Z"), "TEST-FX"));
        AtivoCarteira brl = posicao("2", "10", "12", OffsetDateTime.now(ZoneOffset.UTC), Moeda.BRL);
        AtivoCarteira usd = posicao("1", "10", "12", OffsetDateTime.now(ZoneOffset.UTC), Moeda.USD);
        when(ativos.findByCarteiraId(1L)).thenReturn(List.of(brl, usd));
        service = new DashboardCarteiraService(carteiras, ativos, exchangeRates);

        DashboardCarteiraResponse dashboard = service.calcular(1L);

        assertThat(dashboard.valorInvestido()).isEqualByComparingTo("70.00");
        assertThat(dashboard.valorAtual()).isEqualByComparingTo("84.00");
        assertThat(dashboard.composicao().get(0).valorInvestido()).isEqualByComparingTo("20.00");
        assertThat(dashboard.composicao().get(1).valorInvestido()).isEqualByComparingTo("10.00");
        assertThat(dashboard.composicao().get(1).valorInvestidoConvertidoBase()).isEqualByComparingTo("50.00");
    }

    @Test
    void naoCalculaTotalConsolidadoQuandoCambioUsdEstaIndisponivel() {
        when(carteiras.existsById(1L)).thenReturn(true);
        when(exchangeRates.usdToBrlContext()).thenReturn(ContextoMonetario.unavailable(
                Moeda.USD, Moeda.BRL, BigDecimal.ONE, "FX_PROVIDER_UNAVAILABLE"));
        when(ativos.findByCarteiraId(1L)).thenReturn(List.of(
                posicao("2", "10", "12", OffsetDateTime.now(ZoneOffset.UTC), Moeda.BRL),
                posicao("1", "10", "12", OffsetDateTime.now(ZoneOffset.UTC), Moeda.USD)));
        service = new DashboardCarteiraService(carteiras, ativos, exchangeRates);

        DashboardCarteiraResponse dashboard = service.calcular(1L);

        assertThat(dashboard.valorInvestido()).isNull();
        assertThat(dashboard.valorAtual()).isNull();
        assertThat(dashboard.resultado()).isNull();
        assertThat(dashboard.rentabilidadePercentual()).isNull();
        assertThat(dashboard.composicao().get(0).valorInvestidoConvertidoBase()).isEqualByComparingTo("20");
        assertThat(dashboard.composicao().get(1).valorInvestidoConvertidoBase()).isNull();
    }

    @Test
    void naoAssumeBrlQuandoMoedaDaPosicaoEstaAusente() {
        when(carteiras.existsById(1L)).thenReturn(true);
        when(ativos.findByCarteiraId(1L)).thenReturn(List.of(
                posicao("1", "10", "12", OffsetDateTime.now(ZoneOffset.UTC), null)));

        DashboardCarteiraResponse dashboard = service.calcular(1L);

        assertThat(dashboard.valorInvestido()).isNull();
        assertThat(dashboard.valorAtual()).isNull();
        assertThat(dashboard.composicao().getFirst().valorInvestido()).isEqualByComparingTo("10");
        assertThat(dashboard.composicao().getFirst().valorInvestidoConvertidoBase()).isNull();
    }

    @Test
    void rejeitaCarteiraInexistente() {
        when(carteiras.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.calcular(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void analisaExposicaoCambialEContaResultadosPositivosNegativosENeutros() {
        when(carteiras.existsById(1L)).thenReturn(true);
        when(exchangeRates.usdToBrlContext()).thenReturn(ContextoMonetario.available(
                Moeda.USD, Moeda.BRL, BigDecimal.ONE, new BigDecimal("5"), new BigDecimal("5"),
                OffsetDateTime.parse("2026-11-15T11:00:00Z"), "TEST-FX"));
        AtivoCarteira lucro = posicao("2", "10", "12", OffsetDateTime.now(ZoneOffset.UTC), Moeda.BRL);
        AtivoCarteira prejuizo = posicao("1", "10", "8", OffsetDateTime.now(ZoneOffset.UTC), Moeda.USD);
        AtivoCarteira neutro = posicao("1", "10", "10", OffsetDateTime.now(ZoneOffset.UTC), Moeda.BRL);
        lucro.setCambioHistoricoComprovado(true);
        prejuizo.setCambioHistoricoComprovado(true);
        neutro.setCambioHistoricoComprovado(true);
        when(ativos.findByCarteiraId(1L)).thenReturn(List.of(lucro, prejuizo, neutro));
        service = new DashboardCarteiraService(carteiras, ativos, exchangeRates);

        var analise = service.analisarMoedas(1L);

        assertThat(analise.quantidadeEmLucro()).isEqualTo(1);
        assertThat(analise.quantidadeEmPrejuizo()).isEqualTo(1);
        assertThat(analise.quantidadeNeutra()).isEqualTo(1);
        assertThat(analise.exposicoes()).hasSize(2);
        assertThat(analise.resultadosPorMoeda()).hasSize(2);
        assertThat(analise.mensagemCambio()).isNull();
    }

    @Test
    void analisaMoedasSemPosicoesOuCambioDisponivelSemInventarTotais() {
        when(carteiras.existsById(1L)).thenReturn(true);
        when(ativos.findByCarteiraId(1L)).thenReturn(List.of());
        var vazia = service.analisarMoedas(1L);
        assertThat(vazia.exposicoes()).isEmpty();
        assertThat(vazia.resultadosPorMoeda()).isEmpty();

        when(exchangeRates.usdToBrlContext()).thenReturn(ContextoMonetario.unavailable(
                Moeda.USD, Moeda.BRL, BigDecimal.ONE, "FX_PROVIDER_UNAVAILABLE"));
        AtivoCarteira usd = posicao("1", "10", "12", OffsetDateTime.now(ZoneOffset.UTC), Moeda.USD);
        when(ativos.findByCarteiraId(1L)).thenReturn(List.of(usd));
        service = new DashboardCarteiraService(carteiras, ativos, exchangeRates);

        var indisponivel = service.analisarMoedas(1L);

        assertThat(indisponivel.mensagemCambio()).isEqualTo("Conversao cambial indisponivel");
        assertThat(indisponivel.exposicoes()).hasSize(1);
        assertThat(indisponivel.exposicoes().getFirst().percentual()).isNull();
    }

    @Test
    void consolidaCustoHistoricoEExcluiPosicaoEncerrada() {
        when(carteiras.existsById(1L)).thenReturn(true);
        AtivoCarteira aberta = posicao("10", "10", "12", OffsetDateTime.now(ZoneOffset.UTC), Moeda.BRL);
        aberta.getAcao().setId(10L);
        AtivoCarteira encerrada = posicao("0", "20", "25", OffsetDateTime.now(ZoneOffset.UTC), Moeda.BRL);
        encerrada.getAcao().setId(20L);
        MovimentacaoFinanceiraRepository movimentacoes = org.mockito.Mockito.mock(MovimentacaoFinanceiraRepository.class);
        when(ativos.findByCarteiraId(1L)).thenReturn(List.of(aberta, encerrada));
        when(movimentacoes.findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscIdAsc(1L, 10L))
                .thenReturn(List.of(movimento(TipoMovimentacao.COMPRA, "10", "10"),
                        movimento(TipoMovimentacao.VENDA, "4", "12")));
        when(movimentacoes.findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscIdAsc(1L, 20L))
                .thenReturn(List.of(movimento(TipoMovimentacao.COMPRA, "2", "20"),
                        movimento(TipoMovimentacao.VENDA, "2", "25")));
        service = new DashboardCarteiraService(carteiras, ativos, null, movimentacoes);

        var dashboard = service.calcular(1L);

        assertThat(dashboard.composicao()).hasSize(1);
        assertThat(dashboard.composicao().getFirst().quantidade()).isEqualByComparingTo("6");
        assertThat(dashboard.composicao().getFirst().precoMedio()).isEqualByComparingTo("10");
    }

    private MovimentacaoFinanceira movimento(TipoMovimentacao tipo, String quantidade, String preco) {
        MovimentacaoFinanceira movimento = new MovimentacaoFinanceira();
        movimento.setTipo(tipo);
        movimento.setQuantidade(new BigDecimal(quantidade));
        movimento.setPrecoUnitario(new BigDecimal(preco));
        return movimento;
    }

    private AtivoCarteira posicao(String quantidade, String precoMedio, String cotacao, OffsetDateTime dataHoraCotacao) {
        return posicao(quantidade, precoMedio, cotacao, dataHoraCotacao, Moeda.BRL);
    }

    private AtivoCarteira posicao(String quantidade, String precoMedio, String cotacao,
            OffsetDateTime dataHoraCotacao, Moeda moeda) {
        Acao acao = new Acao();
        acao.setMoeda(moeda);
        acao.setCotacaoAtual(cotacao == null ? null : new BigDecimal(cotacao));
        acao.setDataHoraCotacao(dataHoraCotacao);
        AtivoCarteira posicao = new AtivoCarteira();
        posicao.setQuantidade(new BigDecimal(quantidade));
        posicao.setPrecoMedio(new BigDecimal(precoMedio));
        posicao.setAcao(acao);
        return posicao;
    }
}
