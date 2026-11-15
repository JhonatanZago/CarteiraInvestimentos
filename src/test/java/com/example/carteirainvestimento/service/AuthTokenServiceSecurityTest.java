package com.example.carteirainvestimento.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.carteirainvestimento.config.AuthProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

class AuthTokenServiceSecurityTest {

    @Test
    void rejectsMissingSecretOutsideTestProfile() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new AuthTokenService(new AuthProperties("", true, "Lax", "/api/v1/auth"), environment));

        assertEquals("AUTH_JWT_SECRET deve ser fornecido fora do perfil de teste", error.getMessage());
    }

    @Test
    void rejectsFormerDefaultSecretOutsideTestProfile() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new AuthTokenService(new AuthProperties("dev-secret-change-this-value-please", true, "Lax", "/api/v1/auth"), environment));

        assertEquals("AUTH_JWT_SECRET não pode usar um valor padrão ou de exemplo", error.getMessage());
    }

    @Test
    void allowsGeneratedInMemorySecretOnlyInTestProfile() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "test");
        AuthTokenService tokens = new AuthTokenService(new AuthProperties("", false, "Lax", "/api/v1/auth"), environment);

        String token = tokens.accessToken(42L, 60);
        assertEquals(42L, tokens.userId(token));
        assertDoesNotThrow(() -> tokens.userId(token));
    }

    @Test
    void rejectsExpiredToken() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "test");
        AuthTokenService tokens = new AuthTokenService(new AuthProperties("", false, "Lax", "/api/v1/auth"), environment);

        assertNull(tokens.userId(tokens.accessToken(42L, -1)));
    }

    @Test
    void rejectsShortSecretOutsideTestProfileWithoutEchoingIt() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");
        String secret = "short-secret";

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new AuthTokenService(new AuthProperties(secret, true, "Lax", "/api/v1/auth"), environment));

        assertEquals("AUTH_JWT_SECRET deve possuir ao menos 32 caracteres", error.getMessage());
        assertFalse(error.getMessage().contains(secret));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "dev-only-change-me-please-123456789",
            "changeme-please-rotate-this-secret-123456"
    })
    void rejectsKnownPlaceholderSecretsOutsideTestProfileWithoutEchoingThem(String secret) {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new AuthTokenService(new AuthProperties(secret, true, "Lax", "/api/v1/auth"), environment));

        assertEquals("AUTH_JWT_SECRET não pode usar um valor padrão ou de exemplo", error.getMessage());
        assertFalse(error.getMessage().contains(secret));
    }

    @Test
    void rejectsMalformedClaimsAndNonHmacAlgorithmTokens() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "test");
        AuthTokenService tokens = new AuthTokenService(new AuthProperties("", false, "Lax", "/api/v1/auth"), environment);

        assertNull(tokens.userId("not-a-token"));
        assertNull(tokens.userId("."));
        assertNull(tokens.userId("eyJhbGciOiJub25lIn0.eyJzdWIiOiIxIn0."));
        assertNull(tokens.userId("invalid-claim.signature"));
    }
}
