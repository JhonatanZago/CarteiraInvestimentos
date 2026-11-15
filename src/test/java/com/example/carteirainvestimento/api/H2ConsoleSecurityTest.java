package com.example.carteirainvestimento.api;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Base64;
import java.util.UUID;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.security.test-permit-all=false",
        "spring.datasource.url=jdbc:h2:mem:h2-console-security;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
class H2ConsoleSecurityTest {

    @Autowired private Environment environment;
    @Value("${local.server.port}") private int port;

    @DynamicPropertySource
    static void runtimeSecret(DynamicPropertyRegistry registry) {
        registry.add("app.auth.jwt-secret", () -> Base64.getUrlEncoder().withoutPadding()
                .encodeToString(UUID.randomUUID().toString().repeat(3).getBytes()));
    }

    @Test
    void localProfileRegistersH2ConsoleOnlyForExplicitLocalDevelopment() throws Exception {
        assertTrue(environment.acceptsProfiles("local"));
        assertTrue(environment.getProperty("spring.h2.console.enabled", Boolean.class, false));
        var response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/h2-console/")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertTrue(response.statusCode() >= 200 && response.statusCode() < 300);
    }
}
