package com.example.carteirainvestimento.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.domain.Corretora;
import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.repository.AcaoRepository;
import com.example.carteirainvestimento.repository.CorretoraRepository;
import com.example.carteirainvestimento.service.AuthTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "app.security.test-permit-all=false",
        "app.auth.refresh-cookie-secure=true",
        "app.auth.refresh-cookie-same-site=Strict",
        "spring.datasource.url=jdbc:h2:mem:negative-authorization-security;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class NegativeAuthorizationSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthTokenService tokens;
    @Autowired private AcaoRepository acoes;
    @Autowired private CorretoraRepository corretoras;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rejectsMissingInvalidAndExpiredCredentialsWithoutInternalDetails() throws Exception {
        assertUnauthorized(mockMvc.perform(get("/api/v1/carteiras"))
                .andExpect(status().isUnauthorized()).andReturn(), null);
        assertUnauthorized(mockMvc.perform(get("/api/v1/carteiras/999999"))
                .andExpect(status().isUnauthorized()).andReturn(), null);
        assertUnauthorized(mockMvc.perform(post("/api/v1/carteiras")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"nao criar\"}"))
                .andExpect(status().isUnauthorized()).andReturn(), null);

        String invalid = "Bearer invalid.jwt.value";
        assertUnauthorized(mockMvc.perform(get("/api/v1/carteiras").header("Authorization", invalid))
                .andExpect(status().isUnauthorized()).andReturn(), invalid);
        String expired = "Bearer " + tokens.accessToken(999999L, -1);
        assertUnauthorized(mockMvc.perform(get("/api/v1/carteiras").header("Authorization", expired))
                .andExpect(status().isUnauthorized()).andReturn(), expired);
    }

    @Test
    void loginSuccessAndFailureDoNotExposeRefreshTokensOrCredentials() throws Exception {
        String email = uniqueEmail("login-negative");
        register(email);

        MvcResult success = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "Senha123!")))
                .andExpect(status().isOk()).andReturn();
        String successBody = success.getResponse().getContentAsString();
        assertTrue(successBody.contains("accessToken"));
        assertFalse(successBody.contains("refreshToken"));
        assertFalse(successBody.contains("Senha123!"));

        MvcResult failure = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "senha-incorreta")))
                .andExpect(status().isBadRequest()).andReturn();
        String failureBody = failure.getResponse().getContentAsString();
        assertFalse(failureBody.contains(email));
        assertFalse(failureBody.contains("senha-incorreta"));
        assertFalse(failureBody.contains("stackTrace"));
        assertFalse(failureBody.contains("java."));
    }

    @Test
    void crossUserIdsReturnNotFoundWithoutPrivateDataOrMutation() throws Exception {
        String emailA = uniqueEmail("negative-a");
        String emailB = uniqueEmail("negative-b");
        register(emailA);
        register(emailB);
        String tokenA = loginToken(emailA);
        String tokenB = loginToken(emailB);
        String privateName = "PRIVATE-A-" + UUID.randomUUID();
        long carteiraA = createPortfolio(tokenA, privateName);
        long carteiraB = createPortfolio(tokenB, "PRIVATE-B-" + UUID.randomUUID());
        Acao acao = createAsset();
        Corretora corretora = createBroker();
        long posicaoA = createPosition(tokenA, carteiraA, acao.getId(), corretora.getId());

        assertForeignNotFound(get("/api/v1/carteiras/{id}", carteiraA), tokenB, privateName);
        assertForeignNotFound(get("/api/v1/carteiras/{id}/posicoes", carteiraA), tokenB, privateName);
        assertForeignNotFound(get("/api/v1/carteiras/{id}/posicoes/paginadas", carteiraA), tokenB, privateName);
        assertForeignNotFound(get("/api/v1/dashboard/carteiras/{id}", carteiraA), tokenB, privateName);
        assertForeignNotFound(get("/api/v1/carteiras/{id}/evolucao", carteiraA), tokenB, privateName);
        assertForeignNotFound(get("/api/v1/carteiras/{id}/proventos", carteiraA), tokenB, privateName);
        assertForeignNotFound(get("/api/v1/carteiras/{id}/analise-moedas", carteiraA), tokenB, privateName);
        assertForeignNotFound(get("/api/v1/carteiras/{id}/vendas", carteiraA), tokenB, privateName);
        assertForeignNotFound(put("/api/v1/carteiras/{id}", carteiraA)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"nao alterar\"}"), tokenB, privateName);
        assertForeignNotFound(delete("/api/v1/carteiras/{id}", carteiraA), tokenB, privateName);
        assertForeignNotFound(post("/api/v1/carteiras/{id}/posicoes", carteiraA)
                .contentType(MediaType.APPLICATION_JSON).content(positionJson(acao.getId(), corretora.getId())), tokenB, privateName);
        assertForeignNotFound(put("/api/v1/carteiras/{carteiraId}/posicoes/{posicaoId}", carteiraA, posicaoA)
                .contentType(MediaType.APPLICATION_JSON).content(positionJson(acao.getId(), corretora.getId())), tokenB, privateName);
        assertForeignNotFound(delete("/api/v1/carteiras/{carteiraId}/posicoes/{posicaoId}", carteiraA, posicaoA), tokenB, privateName);
        assertForeignNotFound(post("/api/v1/carteiras/{id}/vendas/simulacao", carteiraB)
                .contentType(MediaType.APPLICATION_JSON).content(saleJson(posicaoA)), tokenB, privateName);
        assertForeignNotFound(post("/api/v1/carteiras/{id}/vendas", carteiraB)
                .contentType(MediaType.APPLICATION_JSON).content(saleJson(posicaoA)), tokenB, privateName);

        assertForeignNotFound(get("/api/v1/carteiras/{id}", 999999999L), tokenB, privateName);
    }

    @Test
    void forbiddenOriginIsDeniedWithoutGrantingCorsOrDetails() throws Exception {
        MvcResult result = mockMvc.perform(options("/api/v1/carteiras")
                        .header("Origin", "https://attacker.example")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isForbidden()).andReturn();
        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("attacker.example"));
        assertFalse(result.getResponse().containsHeader("Access-Control-Allow-Credentials"));
    }

    private void assertUnauthorized(MvcResult result, String credential) throws Exception {
        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("stackTrace"));
        assertFalse(body.contains("java."));
        if (credential != null) assertFalse(body.contains(credential));
    }

    private void assertForeignNotFound(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                       String token, String privateName) throws Exception {
        MvcResult result = mockMvc.perform(request.header("Authorization", bearer(token)))
                .andExpect(status().isNotFound()).andReturn();
        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains(privateName));
        assertFalse(body.contains("stackTrace"));
        assertFalse(body.contains("java."));
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Usuario\",\"email\":\"" + email
                                + "\",\"senha\":\"Senha123!\",\"confirmacaoSenha\":\"Senha123!\"}"))
                .andExpect(status().isCreated());
    }

    private String loginToken(String email) throws Exception {
        JsonNode json = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginJson(email, "Senha123!")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    private long createPortfolio(String token, String name) throws Exception {
        JsonNode json = objectMapper.readTree(mockMvc.perform(post("/api/v1/carteiras")
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + name + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    private long createPosition(String token, long carteiraId, long acaoId, long corretoraId) throws Exception {
        JsonNode json = objectMapper.readTree(mockMvc.perform(post("/api/v1/carteiras/{id}/posicoes", carteiraId)
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(positionJson(acaoId, corretoraId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    private Acao createAsset() {
        Acao acao = new Acao();
        acao.setTicker("NEG" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        acao.setNomeEmpresa("Ativo privado negativo");
        acao.setMercado(Mercado.BRASIL);
        acao.setMoeda(Moeda.BRL);
        acao.setCotacaoAtual(new BigDecimal("25"));
        acao.setDataHoraCotacao(OffsetDateTime.now(ZoneOffset.UTC));
        return acoes.save(acao);
    }

    private Corretora createBroker() {
        Corretora corretora = new Corretora();
        corretora.setCnpj("8" + UUID.randomUUID().toString().replace("-", "").substring(0, 13));
        corretora.setRazaoSocial("Corretora negativa");
        corretora.setValidadaMercadoFinanceiro(true);
        corretora.setDataCadastro(OffsetDateTime.now(ZoneOffset.UTC));
        return corretoras.save(corretora);
    }

    private String positionJson(long acaoId, long corretoraId) {
        return "{\"acaoId\":" + acaoId + ",\"corretoraId\":" + corretoraId
                + ",\"quantidade\":10,\"precoMedio\":20,\"dataPrimeiraCompra\":\"2026-08-01\"}";
    }

    private String saleJson(long posicaoId) {
        return "{\"posicaoId\":" + posicaoId
                + ",\"quantidade\":1,\"precoVenda\":25,\"taxas\":0,\"dataVenda\":\"2026-11-11\",\"moeda\":\"BRL\"}";
    }

    private String loginJson(String email, String password) {
        return "{\"email\":\"" + email + "\",\"senha\":\"" + password + "\",\"manterConectado\":false}";
    }

    private String bearer(String token) { return "Bearer " + token; }
    private String uniqueEmail(String prefix) { return prefix + "+" + UUID.randomUUID() + "@example.com"; }
}
