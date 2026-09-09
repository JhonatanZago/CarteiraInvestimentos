package com.example.carteirainvestimento.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.domain.AtivoCarteira;
import com.example.carteirainvestimento.domain.Carteira;
import com.example.carteirainvestimento.domain.Corretora;
import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.repository.AcaoRepository;
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.repository.CarteiraRepository;
import com.example.carteirainvestimento.repository.CorretoraRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import com.example.carteirainvestimento.repository.PortfolioSnapshotRepository;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest {

    private static final AtomicLong SEQUENCE = new AtomicLong(10_000);

    @Autowired private MockMvc mockMvc;
    @Autowired private CarteiraRepository carteiras;
    @Autowired private AtivoCarteiraRepository ativos;
    @Autowired private PortfolioSnapshotRepository snapshots;
    @Autowired private AcaoRepository acoes;
    @Autowired private CorretoraRepository corretoras;

    @BeforeEach
    void limparCarteiras() {
        ativos.deleteAll();
        snapshots.deleteAll();
        carteiras.deleteAll();
    }

    @Test
    void retornaIndicadoresDaFixturePersistida() throws Exception {
        Carteira carteira = new Carteira();
        carteira.setNome("Dashboard");
        carteira.setDataCriacao(OffsetDateTime.of(2026, 8, 26, 10, 0, 0, 0, ZoneOffset.UTC));
        carteira = carteiras.save(carteira);

        OffsetDateTime cotacaoEm = OffsetDateTime.of(2026, 8, 26, 12, 0, 0, 0, ZoneOffset.UTC);
        Acao acao = criarAcao(cotacaoEm);
        Corretora corretora = criarCorretora();
        AtivoCarteira posicao = new AtivoCarteira();
        posicao.setCarteira(carteira);
        posicao.setAcao(acao);
        posicao.setCorretora(corretora);
        posicao.setQuantidade(new BigDecimal("4"));
        posicao.setPrecoMedio(new BigDecimal("10.00"));
        posicao.setDataPrimeiraCompra(cotacaoEm.toLocalDate());
        ativos.save(posicao);

        mockMvc.perform(get("/api/v1/dashboard/carteiras/{id}", carteira.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carteiraId").value(carteira.getId()))
                .andExpect(jsonPath("$.valorInvestido").value(40.00))
                .andExpect(jsonPath("$.valorAtual").value(60.00))
                .andExpect(jsonPath("$.resultado").value(20.00))
                .andExpect(jsonPath("$.rentabilidadePercentual").value(50.0000))
                .andExpect(jsonPath("$.ultimaAtualizacao").value("2026-08-26T12:00:00Z"))
                .andExpect(jsonPath("$.quantidadeAtivos").value(1))
                .andExpect(jsonPath("$.composicao[0].ticker").value(acao.getTicker()))
                .andExpect(jsonPath("$.composicao[0].valorInvestido").value(40.00))
                .andExpect(jsonPath("$.composicao[0].valorAtual").value(60.00))
                .andExpect(jsonPath("$.composicao[0].resultado").value(20.00));
    }

    @Test
    void serializaComposicaoVaziaParaCarteiraSemPosicoes() throws Exception {
        Carteira carteira = new Carteira();
        carteira.setNome("Sem posicoes");
        carteira.setDataCriacao(OffsetDateTime.now(ZoneOffset.UTC));
        carteira = carteiras.save(carteira);

        mockMvc.perform(get("/api/v1/dashboard/carteiras/{id}", carteira.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeAtivos").value(0))
                .andExpect(jsonPath("$.composicao").isEmpty());
    }

    private Acao criarAcao(OffsetDateTime cotacaoEm) {
        long sequence = SEQUENCE.incrementAndGet();
        Acao acao = new Acao();
        acao.setTicker("D" + sequence + "ASH");
        acao.setNomeEmpresa("Acao dashboard " + sequence);
        acao.setMercado(Mercado.BRASIL);
        acao.setMoeda(Moeda.BRL);
        acao.setCotacaoAtual(new BigDecimal("15.00"));
        acao.setDataHoraCotacao(cotacaoEm);
        return acoes.save(acao);
    }

    private Corretora criarCorretora() {
        long sequence = SEQUENCE.incrementAndGet();
        Corretora corretora = new Corretora();
        corretora.setCnpj(String.format("%014d", sequence));
        corretora.setRazaoSocial("Corretora dashboard " + sequence);
        corretora.setValidadaMercadoFinanceiro(true);
        corretora.setDataCadastro(OffsetDateTime.now(ZoneOffset.UTC));
        return corretoras.save(corretora);
    }
}
