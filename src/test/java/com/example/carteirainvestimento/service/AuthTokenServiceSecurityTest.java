package com.example.carteirainvestimento.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.carteirainvestimento.config.AuthProperties;
import com.example.carteirainvestimento.enums.PerfilUsuario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

    @Test
    void emitsStandardHs256JwtClaimsWithoutSensitiveData() throws Exception {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "test");
        AuthTokenService tokens = new AuthTokenService(
                new AuthProperties("abcdefghijklmnopqrstuvwxyz123456", "carteira-investimento", false, "Lax", "/api/v1/auth"),
                environment);

        String token = tokens.accessToken(42L, PerfilUsuario.USER, 60);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode header = mapper.readTree(new String(
                Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8));
        JsonNode claims = mapper.readTree(new String(
                Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));
        assertEquals("HS256", header.get("alg").asText());
        assertEquals("JWT", header.get("typ").asText());
        assertEquals("42", claims.get("sub").asText());
        assertEquals("carteira-investimento", claims.get("iss").asText());
        assertTrue(claims.has("iat"));
        assertTrue(claims.has("exp"));
        assertEquals("ROLE_USER", claims.get("authorities").get(0).asText());
        assertFalse(claims.has("senha"));
        assertFalse(claims.has("password"));
        assertFalse(claims.has("hash"));
        assertFalse(claims.has("saldo"));
        assertEquals(42L, tokens.userId(token));
    }

    @Test
    void rejectsTokenWithUnexpectedIssuer() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "test");
        String secret = "abcdefghijklmnopqrstuvwxyz123456";
        AuthTokenService expectedIssuer = new AuthTokenService(
                new AuthProperties(secret, "carteira-investimento", false, "Lax", "/api/v1/auth"), environment);
        AuthTokenService unexpectedIssuer = new AuthTokenService(
                new AuthProperties(secret, "outro-emissor", false, "Lax", "/api/v1/auth"), environment);

        assertNull(expectedIssuer.userId(unexpectedIssuer.accessToken(42L, 60)));
    }
}
