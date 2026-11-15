package com.example.carteirainvestimento.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.domain.Corretora;
import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.repository.AcaoRepository;
import com.example.carteirainvestimento.repository.CorretoraRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.security.test-permit-all=false",
        "app.auth.refresh-cookie-secure=true",
        "app.auth.refresh-cookie-same-site=Strict",
        "spring.datasource.url=jdbc:h2:mem:financial-ownership-security;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class FinancialOwnershipSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AcaoRepository acoes;
    @Autowired private CorretoraRepository corretoras;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void isolatesPrivateFinancialResourcesAcrossTwoAuthenticatedOwners() throws Exception {
        String emailA = uniqueEmail("owner-a");
        String emailB = uniqueEmail("owner-b");
        register(emailA);
        register(emailB);
        String tokenA = loginToken(emailA);
        String tokenB = loginToken(emailB);

        long carteiraA = createPortfolio(tokenA, "Carteira privada A");
        long carteiraB = createPortfolio(tokenB, "Carteira privada B");
        long insightsA = createPortfolio(tokenA, "Insights privados A");
        Acao acao = createAsset();
        Corretora corretora = createBroker();
        long posicaoA = createPosition(tokenA, carteiraA, acao.getId(), corretora.getId(), "10", "20");
        long posicaoB = createPosition(tokenB, carteiraB, acao.getId(), corretora.getId(), "7", "30");

        mockMvc.perform(get("/api/v1/carteiras/{id}", carteiraA).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk()).andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertTrue(result.getResponse().getContentAsString().contains("Carteira privada A")));
        mockMvc.perform(get("/api/v1/carteiras/{id}/posicoes", carteiraB).header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk()).andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    org.junit.jupiter.api.Assertions.assertTrue(body.contains("7"));
                    org.junit.jupiter.api.Assertions.assertFalse(body.contains("Carteira privada A"));
                });
        mockMvc.perform(get("/api/v1/dashboard/carteiras/{id}", carteiraA).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/carteiras/{id}/analise-moedas", carteiraA).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/carteiras/{id}/evolucao", insightsA).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/carteiras/{id}/proventos", insightsA).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk());

        String foreign = bearer(tokenB);
        mockMvc.perform(get("/api/v1/carteiras/{id}", carteiraA).header("Authorization", foreign)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/carteiras/{id}/posicoes", carteiraA).header("Authorization", foreign)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/carteiras/{id}/posicoes/paginadas", carteiraA).header("Authorization", foreign)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/dashboard/carteiras/{id}", carteiraA).header("Authorization", foreign)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/carteiras/{id}/analise-moedas", carteiraA).header("Authorization", foreign)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/carteiras/{id}/evolucao", carteiraA).header("Authorization", foreign)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/carteiras/{id}/proventos", carteiraA).header("Authorization", foreign)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/carteiras/{id}/vendas", carteiraA).header("Authorization", foreign)).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/carteiras/{id}", carteiraA).header("Authorization", foreign)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"nao alterar\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/carteiras/{id}", carteiraA).header("Authorization", foreign)).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/carteiras/{id}/posicoes", carteiraA).header("Authorization", foreign)
                        .contentType(MediaType.APPLICATION_JSON).content(positionJson(acao.getId(), corretora.getId(), "1", "99")))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/carteiras/{carteiraId}/posicoes/{posicaoId}", carteiraA, posicaoA)
                        .header("Authorization", foreign).contentType(MediaType.APPLICATION_JSON)
                        .content(positionJson(acao.getId(), corretora.getId(), "99", "99")))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/carteiras/{carteiraId}/posicoes/{posicaoId}", carteiraA, posicaoA)
                        .header("Authorization", foreign)).andExpect(status().isNotFound());
        String saleJson = "{\"posicaoId\":" + posicaoA + ",\"quantidade\":\"1\",\"precoVenda\":\"25\",\"taxas\":\"0\",\"dataVenda\":\"2026-11-11\",\"moeda\":\"BRL\"}";
        mockMvc.perform(post("/api/v1/carteiras/{id}/vendas/simulacao", carteiraA).header("Authorization", foreign)
                        .contentType(MediaType.APPLICATION_JSON).content(saleJson)).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/carteiras/{id}/vendas", carteiraA).header("Authorization", foreign)
                        .contentType(MediaType.APPLICATION_JSON).content(saleJson)).andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/carteiras").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk()).andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    org.junit.jupiter.api.Assertions.assertTrue(body.contains("Carteira privada A"));
                    org.junit.jupiter.api.Assertions.assertFalse(body.contains("Carteira privada B"));
                });
        mockMvc.perform(get("/api/v1/carteiras").header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk()).andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    org.junit.jupiter.api.Assertions.assertTrue(body.contains("Carteira privada B"));
                    org.junit.jupiter.api.Assertions.assertFalse(body.contains("Carteira privada A"));
                });
        mockMvc.perform(post("/api/v1/carteiras/{id}/vendas", carteiraA).header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON).content(saleJson)).andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/carteiras/{id}/vendas", carteiraA).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk()).andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertTrue(result.getResponse().getContentAsString().contains(posicaoA + "")));
        mockMvc.perform(get("/api/v1/carteiras/{id}/posicoes", carteiraB).header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk()).andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertTrue(result.getResponse().getContentAsString().contains(posicaoB + "")));
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Usuario\",\"email\":\"" + email
                                + "\",\"senha\":\"Senha123!\",\"confirmacaoSenha\":\"Senha123!\"}"))
                .andExpect(status().isCreated());
    }

    private String loginToken(String email) throws Exception {
        JsonNode json = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"Senha123!\",\"manterConectado\":false}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    private long createPortfolio(String token, String name) throws Exception {
        JsonNode json = objectMapper.readTree(mockMvc.perform(post("/api/v1/carteiras").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"" + name + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    private long createPosition(String token, long carteiraId, long acaoId, long corretoraId, String quantidade, String preco) throws Exception {
        JsonNode json = objectMapper.readTree(mockMvc.perform(post("/api/v1/carteiras/{id}/posicoes", carteiraId)
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(positionJson(acaoId, corretoraId, quantidade, preco)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    private Acao createAsset() {
        Acao acao = new Acao();
        acao.setTicker("OWN" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        acao.setNomeEmpresa("Ativo privado de teste");
        acao.setMercado(Mercado.BRASIL);
        acao.setMoeda(Moeda.BRL);
        acao.setCotacaoAtual(new BigDecimal("25"));
        acao.setDataHoraCotacao(OffsetDateTime.now(ZoneOffset.UTC));
        return acoes.save(acao);
    }

    private Corretora createBroker() {
        Corretora corretora = new Corretora();
        corretora.setCnpj("9" + UUID.randomUUID().toString().replace("-", "").substring(0, 13));
        corretora.setRazaoSocial("Corretora privada de teste");
        corretora.setValidadaMercadoFinanceiro(true);
        corretora.setDataCadastro(OffsetDateTime.now(ZoneOffset.UTC));
        return corretoras.save(corretora);
    }

    private String positionJson(long acaoId, long corretoraId, String quantidade, String preco) {
        return "{\"acaoId\":" + acaoId + ",\"corretoraId\":" + corretoraId
                + ",\"quantidade\":" + quantidade + ",\"precoMedio\":" + preco
                + ",\"dataPrimeiraCompra\":\"2026-08-01\"}";
    }

    private String bearer(String token) { return "Bearer " + token; }
    private String uniqueEmail(String prefix) { return prefix + "+" + UUID.randomUUID() + "@example.com"; }
}
