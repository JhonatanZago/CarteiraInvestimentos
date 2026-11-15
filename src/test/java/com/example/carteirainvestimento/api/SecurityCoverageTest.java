package com.example.carteirainvestimento.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.carteirainvestimento.repository.RefreshTokenRepository;
import com.example.carteirainvestimento.service.AuthTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "app.security.test-permit-all=false",
        "app.auth.refresh-cookie-secure=true",
        "app.auth.refresh-cookie-same-site=Strict"
})
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class SecurityCoverageTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthTokenService tokens;
    @Autowired private RefreshTokenRepository refreshTokens;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rejectsTamperedSignatureWithoutEchoingCredential() throws Exception {
        String token = tokens.accessToken(999L, 60);
        String[] tokenParts = token.split("\\.");
        String signature = tokenParts[2];
        String tamperedSignature = (signature.charAt(0) == 'A' ? "B" : "A") + signature.substring(1);
        String tampered = tokenParts[0] + "." + tokenParts[1] + "." + tamperedSignature;

        mockMvc.perform(get("/api/v1/carteiras")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertTrue(!result.getResponse().getContentAsString().contains(tampered)));
    }

    @Test
    void securityContextDoesNotLeakBetweenRequests() throws Exception {
        String email = uniqueEmail("context");
        register(email);
        String token = loginToken(email);

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotatesRefreshTokenAndRejectsReuse() throws Exception {
        String email = uniqueEmail("rotation");
        register(email);
        Cookie original = loginCookie(email);

        assertTrue(refreshTokens.findByTokenHash(tokens.hash(original.getValue())).isPresent());
        refreshTokens.findByTokenHash(tokens.hash(original.getValue()))
                .ifPresent(entity -> assertNotEquals(original.getValue(), entity.getTokenHash()));

        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh").cookie(original))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andReturn();
        Cookie replacement = refreshed.getResponse().getCookie("refresh_token");

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(original))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(replacement))
                .andExpect(status().isOk());
    }

    @Test
    void logoutRevokesRefreshTokenAndClearsCookie() throws Exception {
        String email = uniqueEmail("logout");
        register(email);
        Cookie cookie = loginCookie(email);

        mockMvc.perform(post("/api/v1/auth/logout").cookie(cookie))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsCorsMethodAndHeaderOutsideConfiguredPolicy() throws Exception {
        mockMvc.perform(options("/api/v1/carteiras")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "PATCH")
                        .header("Access-Control-Request-Headers", "X-Not-Allowed"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Access-Control-Allow-Origin", not(containsString("*"))));
    }

    @Test
    void isolatesPortfolioCrudAndPositionsFromSecondUser() throws Exception {
        String owner = uniqueEmail("owner");
        String other = uniqueEmail("other");
        register(owner);
        String ownerToken = loginToken(owner);
        long portfolioId = createPortfolio(ownerToken);
        register(other);
        String otherToken = loginToken(other);

        mockMvc.perform(get("/api/v1/carteiras/{id}", portfolioId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/carteiras/{id}", portfolioId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"nao alterar\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/carteiras/{id}", portfolioId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/carteiras/{id}/posicoes", portfolioId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/carteiras/{id}/posicoes", portfolioId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acaoId\":1,\"corretoraId\":1,\"quantidade\":1,\"precoMedio\":10,\"dataPrimeiraCompra\":\"2026-08-01\"}"))
                .andExpect(status().isNotFound());
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Usuario\",\"email\":\"" + email
                                + "\",\"senha\":\"Senha123!\",\"confirmacaoSenha\":\"Senha123!\"}"))
                .andExpect(status().isCreated());
    }

    private String loginToken(String email) throws Exception {
        JsonNode json = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    private Cookie loginCookie(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("refresh_token");
    }

    private long createPortfolio(String token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/carteiras")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Privada\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private String loginJson(String email) {
        return "{\"email\":\"" + email + "\",\"senha\":\"Senha123!\",\"manterConectado\":false}";
    }

    private String uniqueEmail(String prefix) {
        return prefix + "+" + UUID.randomUUID() + "@example.com";
    }
}
