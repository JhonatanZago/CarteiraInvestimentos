package com.example.carteirainvestimento.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.carteirainvestimento.repository.RefreshTokenRepository;
import com.example.carteirainvestimento.service.AuthTokenService;
import jakarta.servlet.http.Cookie;
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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "app.security.test-permit-all=false",
        "app.auth.refresh-cookie-secure=false",
        "app.auth.refresh-cookie-same-site=Lax",
        "app.auth.refresh-cookie-path=/api/v1/auth"
})
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:refresh-cookie-local;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class RefreshCookieLocalSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthTokenService tokens;
    @Autowired private RefreshTokenRepository refreshTokens;

    @Test
    void localLoginRefreshAndLogoutUseHttpOnlyLaxCookieWithoutSecure() throws Exception {
        String email = uniqueEmail("local");
        register(email);

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=86400")))
                .andExpect(header().string("Set-Cookie", not(containsString("Secure"))))
                .andReturn();
        Cookie original = requireCookie(login);
        String loginBody = login.getResponse().getContentAsString();
        assertNotNull(loginBody);
        org.junit.jupiter.api.Assertions.assertFalse(loginBody.contains(original.getValue()));

        MvcResult refresh = mockMvc.perform(post("/api/v1/auth/refresh").cookie(original))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")))
                .andExpect(header().string("Set-Cookie", not(containsString("Secure"))))
                .andReturn();
        Cookie replacement = requireCookie(refresh);
        org.junit.jupiter.api.Assertions.assertNotEquals(original.getValue(), replacement.getValue());

        mockMvc.perform(post("/api/v1/auth/logout").cookie(replacement))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")));
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(replacement))
                .andExpect(status().isBadRequest());
    }

    @Test
    void localRefreshRejectsMissingExpiredAndTamperedCookies() throws Exception {
        String email = uniqueEmail("invalid");
        register(email);
        Cookie cookie = requireCookie(login(email));

        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", cookie.getValue() + "tampered")))
                .andExpect(status().isBadRequest());

        var entity = refreshTokens.findByTokenHash(tokens.hash(cookie.getValue())).orElseThrow();
        entity.setExpiraEm(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        refreshTokens.saveAndFlush(entity);
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie))
                .andExpect(status().isBadRequest());
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
                        .content("{\"nome\":\"Local\",\"email\":\"" + email
                                + "\",\"senha\":\"Senha123!\",\"confirmacaoSenha\":\"Senha123!\"}"))
                .andExpect(status().isCreated());
    }

    private Cookie requireCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie("refresh_token");
        assertNotNull(cookie);
        return cookie;
    }

    private String loginJson(String email) {
        return "{\"email\":\"" + email + "\",\"senha\":\"Senha123!\",\"manterConectado\":false}";
    }

    private String uniqueEmail(String prefix) {
        return prefix + "+" + UUID.randomUUID() + "@example.com";
    }
}
