package com.example.carteirainvestimento.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.carteirainvestimento.service.AuthTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
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
class SecurityRegressionTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired private AuthTokenService tokens;

    @BeforeEach
    void contextIsReady() {
        assertNotNull(mockMvc);
    }

    @Test
    void rejectsMissingAndInvalidAccessTokens() throws Exception {
        mockMvc.perform(get("/api/v1/carteiras"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mockMvc.perform(get("/api/v1/carteiras").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void rejectsExpiredAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/carteiras")
                        .header("Authorization", "Bearer " + tokens.accessToken(999L, -1)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void loginSetsProtectedRefreshCookieAndInvalidLoginFails() throws Exception {
        register("valid@example.com");

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("valid@example.com", "Senha123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("Secure")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")))
                .andReturn();
        assertNotNull(login.getResponse().getHeader("Set-Cookie"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("valid@example.com", "senha-incorreta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void cookieAloneCannotMutateFinancialResource() throws Exception {
        register("cookie@example.com");
        Cookie refreshCookie = loginCookie("cookie@example.com");

        mockMvc.perform(post("/api/v1/carteiras")
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Não deve criar\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void isolatesPortfolioBetweenAuthenticatedUsers() throws Exception {
        register("owner@example.com");
        String ownerToken = loginToken("owner@example.com");
        long portfolioId = createPortfolio(ownerToken, "Carteira privada");

        register("other@example.com");
        String otherToken = loginToken("other@example.com");

        mockMvc.perform(get("/api/v1/carteiras/{id}", portfolioId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void allowsConfiguredOriginAndRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/carteiras")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Authorization")));

        mockMvc.perform(options("/api/v1/carteiras")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Access-Control-Allow-Origin", not(containsString("evil.example"))));
    }

    @Test
    void meEndpointIsNotPublic() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Usuário\",\"email\":\"" + email
                                + "\",\"senha\":\"Senha123!\",\"confirmacaoSenha\":\"Senha123!\"}"))
                .andExpect(status().isCreated());
    }

    private String loginToken(String email) throws Exception {
        JsonNode json = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "Senha123!")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    private Cookie loginCookie(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "Senha123!")))
                .andExpect(status().isOk()).andReturn();
        Cookie cookie = result.getResponse().getCookie("refresh_token");
        assertNotNull(cookie);
        return cookie;
    }

    private long createPortfolio(String token, String name) throws Exception {
        JsonNode json = objectMapper.readTree(mockMvc.perform(post("/api/v1/carteiras")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + name + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    private String loginJson(String email, String password) {
        return "{\"email\":\"" + email + "\",\"senha\":\"" + password + "\",\"manterConectado\":false}";
    }
}
