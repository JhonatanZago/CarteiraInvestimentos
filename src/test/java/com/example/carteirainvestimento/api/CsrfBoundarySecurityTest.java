package com.example.carteirainvestimento.api;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        "app.auth.refresh-cookie-same-site=Strict",
        "app.auth.refresh-cookie-path=/api/v1/auth",
        "app.cors.allowed-origins=http://localhost:4200,http://127.0.0.1:4200"
})
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:csrf-boundary;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class CsrfBoundarySecurityTest {

    private static final String TRUSTED_ORIGIN = "http://localhost:4200";
    private static final String EVIL_ORIGIN = "http://evil.example";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private MockMvc mockMvc;

    @Test
    void refreshCookieCannotAuthorizeFinancialMutationAcrossContentTypes() throws Exception {
        String email = uniqueEmail("cookie-only");
        register(email);
        Cookie refreshCookie = login(email).getResponse().getCookie("refresh_token");
        assertNotNull(refreshCookie);

        mockMvc.perform(post("/api/v1/carteiras")
                        .header("Origin", EVIL_ORIGIN)
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"nao criar\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/carteiras")
                        .header("Origin", EVIL_ORIGIN)
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("nome=nao-criar"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/carteiras")
                        .header("Origin", "null")
                        .cookie(refreshCookie)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("nao criar"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/carteiras")
                        .header("Origin", EVIL_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"nao criar\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/carteiras/999999")
                        .header("Origin", EVIL_ORIGIN)
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"nao alterar\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/carteiras/999999")
                        .header("Origin", EVIL_ORIGIN)
                        .cookie(refreshCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void bearerAuthenticationRemainsFunctionalAndCookieAloneCannotReadFinancialData() throws Exception {
        String email = uniqueEmail("bearer");
        register(email);
        MvcResult login = login(email);
        Cookie refreshCookie = login.getResponse().getCookie("refresh_token");
        String accessToken = objectMapper.readTree(login.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/v1/carteiras")
                        .header("Origin", TRUSTED_ORIGIN)
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", TRUSTED_ORIGIN));
        mockMvc.perform(post("/api/v1/carteiras")
                        .header("Origin", TRUSTED_ORIGIN)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Bearer valido\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/carteiras")
                        .header("Origin", EVIL_ORIGIN)
                        .cookie(refreshCookie))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void refreshAndLogoutAreCookieEndpointsAndDoNotAuthorizeBusinessState() throws Exception {
        String email = uniqueEmail("session-boundary");
        register(email);
        MvcResult login = login(email);
        Cookie original = login.getResponse().getCookie("refresh_token");
        assertNotNull(original);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Origin", EVIL_ORIGIN)
                        .cookie(original))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        MvcResult refresh = mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Origin", TRUSTED_ORIGIN)
                        .cookie(original))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", TRUSTED_ORIGIN))
                .andReturn();
        Cookie successor = refresh.getResponse().getCookie("refresh_token");
        assertNotNull(successor);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Origin", "null")
                        .cookie(successor))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Origin", TRUSTED_ORIGIN)
                        .cookie(successor))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Access-Control-Allow-Origin", TRUSTED_ORIGIN));
        mockMvc.perform(get("/api/v1/carteiras").cookie(successor))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginCsrfRequiresJsonAndUnknownOriginCannotReadTheResponse() throws Exception {
        String email = uniqueEmail("login-csrf");
        register(email);
        String json = loginJson(email);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("email=" + email + "&senha=Senha123%21&manterConectado=false"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(header().doesNotExist("Set-Cookie"));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(json))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(header().doesNotExist("Set-Cookie"));
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Origin", EVIL_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void trustedOriginPreflightDoesNotChangeBearerCookieBoundary() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/v1/carteiras")
                        .header("Origin", TRUSTED_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Origin", TRUSTED_ORIGIN));
    }

    private MvcResult login(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"CSRF\",\"email\":\"" + email
                                + "\",\"senha\":\"Senha123!\",\"confirmacaoSenha\":\"Senha123!\"}"))
                .andExpect(status().isCreated());
    }

    private String loginJson(String email) {
        return "{\"email\":\"" + email + "\",\"senha\":\"Senha123!\",\"manterConectado\":false}";
    }

    private String uniqueEmail(String prefix) {
        return prefix + "+" + UUID.randomUUID() + "@example.com";
    }
}
