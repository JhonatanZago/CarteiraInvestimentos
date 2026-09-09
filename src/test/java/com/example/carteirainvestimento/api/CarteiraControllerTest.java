package com.example.carteirainvestimento.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.domain.Corretora;
import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.repository.AcaoRepository;
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.repository.CarteiraRepository;
import com.example.carteirainvestimento.repository.CorretoraRepository;
import com.example.carteirainvestimento.repository.PortfolioSnapshotRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CarteiraControllerTest {

    private static final AtomicLong SEQUENCE = new AtomicLong();

    @Autowired private MockMvc mockMvc;
    @Autowired private AcaoRepository acoes;
    @Autowired private CorretoraRepository corretoras;
    @Autowired private CarteiraRepository carteiras;
    @Autowired private AtivoCarteiraRepository ativos;
    @Autowired private PortfolioSnapshotRepository snapshots;

    @BeforeEach
    void limparCarteiras() {
        ativos.deleteAll();
        snapshots.deleteAll();
        carteiras.deleteAll();
    }

    @Test
    void executaCrudDeCarteiraEPosicao() throws Exception {
        long carteiraId = criarCarteira("Longo Prazo");
        Acao acao = criarAcao();
        Corretora corretora = criarCorretora();

        mockMvc.perform(get("/api/v1/carteiras/{id}", carteiraId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Longo Prazo"));
        mockMvc.perform(put("/api/v1/carteiras/{id}", carteiraId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Aposentadoria\",\"descricao\":\"Atualizada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Aposentadoria"));

        long posicaoId = criarPosicao(carteiraId, acao.getId(), corretora.getId());
        mockMvc.perform(get("/api/v1/carteiras/{id}/posicoes", carteiraId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(posicaoId))
                .andExpect(jsonPath("$[0].ticker").value(acao.getTicker()))
                .andExpect(jsonPath("$[0].cotacaoAtual").value(10.00))
                .andExpect(jsonPath("$[0].valorInvestido").value(250.00))
                .andExpect(jsonPath("$[0].valorAtual").value(100.00))
                .andExpect(jsonPath("$[0].resultado").value(-150.00))
                .andExpect(jsonPath("$[0].rentabilidadePercentual").value(-60.0000));
        mockMvc.perform(put("/api/v1/carteiras/{carteiraId}/posicoes/{posicaoId}", carteiraId, posicaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(posicaoJson(acao.getId(), corretora.getId(), "12.5", "30.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(12.5));
        mockMvc.perform(delete("/api/v1/carteiras/{carteiraId}/posicoes/{posicaoId}", carteiraId, posicaoId))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/carteiras/{id}", carteiraId))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejeitaDadosInvalidosEPosicaoDuplicada() throws Exception {
        long carteiraId = criarCarteira("Regras");
        Acao acao = criarAcao();
        Corretora corretora = criarCorretora();

        mockMvc.perform(post("/api/v1/carteiras/{id}/posicoes", carteiraId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(posicaoJson(acao.getId(), corretora.getId(), "0", "10.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        criarPosicao(carteiraId, acao.getId(), corretora.getId());
        mockMvc.perform(post("/api/v1/carteiras/{id}/posicoes", carteiraId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(posicaoJson(acao.getId(), corretora.getId(), "10", "25.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void impedeExcluirCarteiraComPosicao() throws Exception {
        long carteiraId = criarCarteira("Protegida");
        Acao acao = criarAcao();
        Corretora corretora = criarCorretora();
        criarPosicao(carteiraId, acao.getId(), corretora.getId());

        mockMvc.perform(delete("/api/v1/carteiras/{id}", carteiraId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    private long criarCarteira(String nome) throws Exception {
        String response = mockMvc.perform(post("/api/v1/carteiras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"descricao\":\"Teste\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1"));
    }

    private long criarPosicao(long carteiraId, long acaoId, long corretoraId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/carteiras/{id}/posicoes", carteiraId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(posicaoJson(acaoId, corretoraId, "10", "25.00")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1"));
    }

    private Acao criarAcao() {
        long sequence = SEQUENCE.incrementAndGet();
        Acao acao = new Acao();
        acao.setTicker("T" + sequence + "ST3");
        acao.setNomeEmpresa("Acao de teste " + sequence);
        acao.setMercado(Mercado.BRASIL);
        acao.setMoeda(Moeda.BRL);
        acao.setCotacaoAtual(BigDecimal.TEN);
        acao.setDataHoraCotacao(OffsetDateTime.now(ZoneOffset.UTC));
        return acoes.save(acao);
    }

    private Corretora criarCorretora() {
        long sequence = SEQUENCE.incrementAndGet();
        Corretora corretora = new Corretora();
        corretora.setCnpj(String.format("%014d", sequence));
        corretora.setRazaoSocial("Corretora de teste " + sequence);
        corretora.setValidadaMercadoFinanceiro(true);
        corretora.setDataCadastro(OffsetDateTime.now(ZoneOffset.UTC));
        return corretoras.save(corretora);
    }

    private String posicaoJson(long acaoId, long corretoraId, String quantidade, String precoMedio) {
        return "{\"acaoId\":" + acaoId + ",\"corretoraId\":" + corretoraId
                + ",\"quantidade\":" + quantidade + ",\"precoMedio\":" + precoMedio
                + ",\"dataPrimeiraCompra\":\"2026-08-25\"}";
    }
}
