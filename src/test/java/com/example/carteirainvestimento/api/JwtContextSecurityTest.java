package com.example.carteirainvestimento.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.carteirainvestimento.domain.Usuario;
import com.example.carteirainvestimento.repository.UsuarioRepository;
import com.example.carteirainvestimento.service.AuthTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
class JwtContextSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthTokenService tokens;
    @Autowired private UsuarioRepository usuarios;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsValidBearerOnlyForTheAuthenticatedRequest() throws Exception {
        String email = uniqueEmail("valid");
        register(email);
        String accessToken = login(email).accessToken();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me").cookie(login(email).refreshCookie()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidAccessTokenVariantsWithUnauthorized() throws Exception {
        String email = uniqueEmail("negative");
        register(email);
        LoginData login = login(email);
        String valid = login.accessToken();
        String[] tokenParts = valid.split("\\.");
        String signature = tokenParts[2];
        String tamperedSignature = (signature.charAt(0) == 'A' ? "B" : "A") + signature.substring(1);
        String tampered = tokenParts[0] + "." + tokenParts[1] + "." + tamperedSignature;

        assertUnauthorized(null);
        assertUnauthorized("");
        assertUnauthorized("Bearer");
        assertUnauthorized("Bearer invalid");
        assertUnauthorized("Bearer " + tokens.accessToken(userId(email), -1));
        assertUnauthorized("Bearer " + tampered);
        assertUnauthorized("Bearer not-a-token");
        assertUnauthorized("Bearer eyJhbGciOiJub25lIn0.eyJzdWIiOiIxIn0.");
        assertUnauthorized("Bearer " + login.refreshCookie().getValue());
    }

    @Test
    void rejectsValidSignatureForUnknownOrInactiveUser() throws Exception {
        assertUnauthorized("Bearer " + tokens.accessToken(999_999_999L, 60));

        String email = uniqueEmail("inactive");
        register(email);
        LoginData login = login(email);
        Usuario user = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        user.setAtivo(false);
        usuarios.saveAndFlush(user);

        assertUnauthorized("Bearer " + login.accessToken());
    }

    @Test
    void keepsSecurityContextIsolatedAcrossParallelRequests() throws Exception {
        String firstEmail = uniqueEmail("parallel-a");
        String secondEmail = uniqueEmail("parallel-b");
        register(firstEmail);
        register(secondEmail);
        String firstToken = login(firstEmail).accessToken();
        String secondToken = login(secondEmail).accessToken();

        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> first = executor.submit(requestFor(start, firstToken));
            Future<MvcResult> second = executor.submit(requestFor(start, secondToken));

            MvcResult firstResult = first.get(10, TimeUnit.SECONDS);
            MvcResult secondResult = second.get(10, TimeUnit.SECONDS);
            assertEquals(200, firstResult.getResponse().getStatus());
            assertEquals(200, secondResult.getResponse().getStatus());
            assertTrue(firstResult.getResponse().getContentAsString().contains(firstEmail));
            assertTrue(secondResult.getResponse().getContentAsString().contains(secondEmail));
            assertTrue(!firstResult.getResponse().getContentAsString().contains(secondEmail));
            assertTrue(!secondResult.getResponse().getContentAsString().contains(firstEmail));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void invalidCredentialsAreNotReturnedInErrorResponses() throws Exception {
        String credential = "Bearer " + tokens.accessToken(123L, 60);
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", credential))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertTrue(!result.getResponse().getContentAsString().contains(credential)));
    }

    @Test
    void mapsPersistedAdminProfileToAdminRoleWithoutGrantingCrossOwnerAccess() throws Exception {
        String email = uniqueEmail("admin");
        register(email);
        Usuario admin = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        admin.setPerfil(com.example.carteirainvestimento.enums.PerfilUsuario.ADMIN);
        usuarios.saveAndFlush(admin);

        LoginData login = login(email);
        JsonNode me = objectMapper.readTree(mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + login.accessToken()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        assertEquals("ADMIN", me.get("perfil").asText());
    }

    private Callable<MvcResult> requestFor(CyclicBarrier start, String token) {
        return () -> {
            start.await(10, TimeUnit.SECONDS);
            return mockMvc.perform(get("/api/v1/auth/me")
                            .header("Authorization", "Bearer " + token))
                    .andReturn();
        };
    }

    private void assertUnauthorized(String authorization) throws Exception {
        var request = mockMvc.perform(get("/api/v1/auth/me"));
        if (authorization != null) {
            request = mockMvc.perform(get("/api/v1/auth/me").header("Authorization", authorization));
        }
        request.andExpect(status().isUnauthorized());
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Usuario\",\"email\":\"" + email
                                + "\",\"senha\":\"Senha123!\",\"confirmacaoSenha\":\"Senha123!\"}"))
                .andExpect(status().isCreated());
    }

    private LoginData login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"Senha123!\",\"manterConectado\":false}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return new LoginData(json.get("accessToken").asText(), result.getResponse().getCookie("refresh_token"));
    }

    private long userId(String email) {
        return usuarios.findByEmailIgnoreCase(email).orElseThrow().getId();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "+" + UUID.randomUUID() + "@example.com";
    }

    private record LoginData(String accessToken, Cookie refreshCookie) {}
}
