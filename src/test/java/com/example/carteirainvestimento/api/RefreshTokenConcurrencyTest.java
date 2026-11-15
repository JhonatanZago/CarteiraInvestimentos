package com.example.carteirainvestimento.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.carteirainvestimento.domain.RefreshToken;
import com.example.carteirainvestimento.repository.RefreshTokenRepository;
import com.example.carteirainvestimento.service.AuthTokenService;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "app.security.test-permit-all=false",
        "app.auth.refresh-cookie-secure=true",
        "app.auth.refresh-cookie-same-site=Strict"
})
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:refresh-concurrency;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class RefreshTokenConcurrencyTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RefreshTokenRepository refreshTokens;
    @Autowired private AuthTokenService tokens;

    @RepeatedTest(10)
    void sameRefreshTokenProducesExactlyOneSuccessor() throws Exception {
        String email = "concurrency+" + UUID.randomUUID() + "@example.com";
        register(email);
        Cookie original = loginCookie(email);
        String originalHash = tokens.hash(original.getValue());
        long userId = refreshTokens.findByTokenHash(originalHash)
                .orElseThrow()
                .getUsuario()
                .getId();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<MvcResult> refresh = () -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return mockMvc.perform(post("/api/v1/auth/refresh").cookie(original)).andReturn();
            };

            var first = executor.submit(refresh);
            var second = executor.submit(refresh);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            MvcResult firstResult = first.get(20, TimeUnit.SECONDS);
            MvcResult secondResult = second.get(20, TimeUnit.SECONDS);
            List<MvcResult> results = List.of(firstResult, secondResult);
            long successes = results.stream().filter(result -> result.getResponse().getStatus() == 200).count();
            long rejections = results.stream().filter(result -> result.getResponse().getStatus() == 400).count();
            assertEquals(1, successes);
            assertEquals(1, rejections);

            MvcResult successful = results.stream()
                    .filter(result -> result.getResponse().getStatus() == 200)
                    .findFirst()
                    .orElseThrow();
            Cookie successor = successful.getResponse().getCookie("refresh_token");
            assertNotNull(successor);

            List<RefreshToken> persisted = refreshTokens.findByUsuarioIdOrderByIdAsc(userId);
            assertEquals(2, persisted.size());
            assertEquals(1, persisted.stream().filter(token -> !token.isUsado()).count());
            assertTrue(refreshTokens.findByTokenHash(originalHash).orElseThrow().isUsado());

            mockMvc.perform(post("/api/v1/auth/refresh").cookie(successor))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/v1/auth/refresh").cookie(original))
                    .andExpect(status().isBadRequest());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/cadastro")
                        .contentType("application/json")
                        .content("{\"nome\":\"Concurrency\",\"email\":\"" + email
                                + "\",\"senha\":\"Senha123!\",\"confirmacaoSenha\":\"Senha123!\"}"))
                .andExpect(status().isCreated());
    }

    private Cookie loginCookie(String email) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"senha\":\"Senha123!\",\"manterConectado\":false}";
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie("refresh_token");
    }
}
