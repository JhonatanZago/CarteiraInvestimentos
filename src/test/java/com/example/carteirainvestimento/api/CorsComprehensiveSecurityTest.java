package com.example.carteirainvestimento.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        "app.cors.allowed-origins=http://localhost:4200,http://127.0.0.1:4200",
        "app.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS",
        "app.cors.allowed-headers=Authorization,Content-Type,Accept,X-Requested-With"
})
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:cors-comprehensive;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class CorsComprehensiveSecurityTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:4200";
    private static final String SECOND_ALLOWED_ORIGIN = "http://127.0.0.1:4200";
    private static final String DISALLOWED_ORIGIN = "http://evil.example";
    private static final String API_PATH = "/api/v1/carteiras";

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsAllowedPreflightWithExplicitMethodsHeadersCredentialsAndVary() throws Exception {
        mockMvc.perform(options(API_PATH)
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Authorization")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Content-Type")))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Vary", containsString("Origin")));
    }

    @Test
    void acceptsBothConfiguredAngularOrigins() throws Exception {
        assertAllowedPreflight(SECOND_ALLOWED_ORIGIN);
    }

    @Test
    void rejectsPreflightForUnknownOriginMethodAndHeader() throws Exception {
        assertNoCorsGrant(mockMvc.perform(options(API_PATH)
                .header("Origin", DISALLOWED_ORIGIN)
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden()));

        assertNoCorsGrant(mockMvc.perform(options(API_PATH)
                .header("Origin", ALLOWED_ORIGIN)
                .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isForbidden()));

        assertNoCorsGrant(mockMvc.perform(options(API_PATH)
                .header("Origin", ALLOWED_ORIGIN)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "X-Not-Allowed"))
                .andExpect(status().isForbidden()));
    }

    @Test
    void rejectsMissingProtocolPortNullAndWildcardOriginsWithoutReflection() throws Exception {
        assertNoCorsGrant(mockMvc.perform(get(API_PATH).header("Origin", "http://localhost:4201")));
        assertNoCorsGrant(mockMvc.perform(get(API_PATH).header("Origin", "https://localhost:4200")));
        assertNoCorsGrant(mockMvc.perform(get(API_PATH).header("Origin", "null")));
        assertNoCorsGrant(mockMvc.perform(get(API_PATH).header("Origin", "*")));
        assertNoCorsGrant(mockMvc.perform(get(API_PATH).header("Origin", DISALLOWED_ORIGIN)));
    }

    @Test
    void originAbsentDoesNotReceiveCorsGrantAndStillFollowsAuthentication() throws Exception {
        mockMvc.perform(get(API_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    void allowedOriginWithoutCredentialsIsNotAuthenticationButIsCorsAllowed() throws Exception {
        mockMvc.perform(get(API_PATH).header("Origin", ALLOWED_ORIGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Vary", containsString("Origin")));
    }

    @Test
    void allowedOriginWithBearerCredentialReachesProtectedEndpoint() throws Exception {
        String email = "cors+" + UUID.randomUUID() + "@example.com";
        register(email);
        String token = loginToken(email);

        mockMvc.perform(get(API_PATH)
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void allowedOriginCoversExistingGetPostPutDeleteRoutesWithoutBypassingAuthentication() throws Exception {
        mockMvc.perform(get(API_PATH).header("Origin", ALLOWED_ORIGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
        mockMvc.perform(post(API_PATH).header("Origin", ALLOWED_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"nao criar\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
        mockMvc.perform(put(API_PATH + "/999999").header("Origin", ALLOWED_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"nao alterar\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
        mockMvc.perform(delete(API_PATH + "/999999").header("Origin", ALLOWED_ORIGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }

    @Test
    void corsDoesNotExposeWildcardOrUnconfiguredHeaders() throws Exception {
        var result = mockMvc.perform(options(API_PATH)
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Accept"))
                .andExpect(status().isOk())
                .andReturn();
        String allowOrigin = result.getResponse().getHeader("Access-Control-Allow-Origin");
        String allowHeaders = result.getResponse().getHeader("Access-Control-Allow-Headers");
        assertFalse("*".equals(allowOrigin));
        assertFalse(allowHeaders != null && allowHeaders.contains("X-Not-Allowed"));
    }

    private void assertAllowedPreflight(String origin) throws Exception {
        mockMvc.perform(options(API_PATH)
                        .header("Origin", origin)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Accept"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Vary", containsString("Origin")));
    }

    private void assertNoCorsGrant(org.springframework.test.web.servlet.ResultActions actions) throws Exception {
        actions.andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Cors\",\"email\":\"" + email
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
}
