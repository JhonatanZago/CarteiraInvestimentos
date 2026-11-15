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
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.repository.CarteiraRepository;
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
