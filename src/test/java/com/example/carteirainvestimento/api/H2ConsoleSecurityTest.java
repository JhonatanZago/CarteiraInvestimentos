package com.example.carteirainvestimento.api;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.security.test-permit-all=false",
        "spring.datasource.url=jdbc:h2:mem:h2-console-security;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
class H2ConsoleSecurityTest {

    @Autowired private MockMvc mockMvc;

    @DynamicPropertySource
    static void runtimeSecret(DynamicPropertyRegistry registry) {
        registry.add("app.auth.jwt-secret", () -> Base64.getUrlEncoder().withoutPadding()
                .encodeToString(UUID.randomUUID().toString().repeat(3).getBytes()));
    }

    @Test
    void localProfileDoesNotExposeH2ConsoleAsUnauthorizedApiRequest() throws Exception {
        int status = mockMvc.perform(get("/h2-console/"))
                .andReturn().getResponse().getStatus();
        assertNotEquals(401, status);
    }
}
