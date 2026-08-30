package com.example.carteirainvestimento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.domain.AtivoCarteira;
import com.example.carteirainvestimento.dto.dashboard.DashboardCarteiraResponse;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
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
    void calculaResultadoNegativoETrataCotacaoAusenteComoValorAtualZero() {
        when(carteiras.existsById(1L)).thenReturn(true);
        when(ativos.findByCarteiraId(1L)).thenReturn(List.of(
                posicao("2", "20", "15", OffsetDateTime.now(ZoneOffset.UTC)),
                posicao("0", "20", "15", OffsetDateTime.now(ZoneOffset.UTC)),
                posicao("2", "20", null, null)));

        DashboardCarteiraResponse dashboard = service.calcular(1L);

        assertThat(dashboard.valorInvestido()).isEqualByComparingTo("80.00");
        assertThat(dashboard.valorAtual()).isEqualByComparingTo("30.00");
        assertThat(dashboard.resultado()).isEqualByComparingTo("-50.00");
        assertThat(dashboard.rentabilidadePercentual()).isEqualByComparingTo("-62.5000");
        assertThat(dashboard.composicao().get(2).valorAtual()).isEqualByComparingTo("0.00");
        assertThat(dashboard.composicao().get(2).rentabilidadePercentual()).isEqualByComparingTo("-100.0000");
    }

    @Test
    void rejeitaCarteiraInexistente() {
        when(carteiras.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.calcular(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private AtivoCarteira posicao(String quantidade, String precoMedio, String cotacao, OffsetDateTime dataHoraCotacao) {
        Acao acao = new Acao();
        acao.setCotacaoAtual(cotacao == null ? null : new BigDecimal(cotacao));
        acao.setDataHoraCotacao(dataHoraCotacao);
        AtivoCarteira posicao = new AtivoCarteira();
        posicao.setQuantidade(new BigDecimal(quantidade));
        posicao.setPrecoMedio(new BigDecimal(precoMedio));
        posicao.setAcao(acao);
        return posicao;
    }
}
