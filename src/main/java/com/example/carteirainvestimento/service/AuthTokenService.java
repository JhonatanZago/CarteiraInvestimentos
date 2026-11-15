package com.example.carteirainvestimento.service;

import com.example.carteirainvestimento.config.AuthProperties;
import com.example.carteirainvestimento.enums.PerfilUsuario;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthTokenService {
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final String issuer;

    public AuthTokenService(AuthProperties properties, Environment environment) {
        boolean test = environment.acceptsProfiles(Profiles.of("test"));
        boolean localProfile = environment.acceptsProfiles(Profiles.of("dev", "local", "test"));
        String configured = properties.jwtSecret();
        if (test && !StringUtils.hasText(configured)) {
            configured = generatedTestSecret();
        }
        if (!StringUtils.hasText(configured)) {
            throw new IllegalStateException("AUTH_JWT_SECRET deve ser fornecido fora do perfil de teste");
        }
        if (configured.length() < 32) {
            throw new IllegalStateException("AUTH_JWT_SECRET deve possuir ao menos 32 caracteres");
        }
        if (!test && isUnsafePlaceholder(configured)) {
            throw new IllegalStateException("AUTH_JWT_SECRET não pode usar um valor padrão ou de exemplo");
        }
        if (!localProfile && !properties.refreshCookieSecure()) {
            throw new IllegalStateException("AUTH_REFRESH_COOKIE_SECURE=true é obrigatório fora dos perfis locais");
        }

        this.issuer = properties.jwtIssuer();
        SecretKey key = new SecretKeySpec(configured.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = NimbusJwtEncoder.withSecretKey(key).build();
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.jwtIssuer()));
        this.decoder = jwtDecoder;
    }

    public String accessToken(Long userId, long ttlSeconds) {
        return accessToken(userId, PerfilUsuario.USER, ttlSeconds);
    }

    public String accessToken(Long userId, PerfilUsuario perfil, long ttlSeconds) {
        Instant issuedAt = ttlSeconds <= 0 ? Instant.now().minusSeconds(121) : Instant.now();
        Instant expiresAt = ttlSeconds <= 0 ? Instant.now().minusSeconds(120) : issuedAt.plusSeconds(ttlSeconds);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(userId))
                .issuer(currentIssuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("authorities", List.of("ROLE_" + (perfil == null ? PerfilUsuario.USER : perfil).name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Long userId(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            return Long.valueOf(jwt.getSubject());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public String hash(String value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    java.security.MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public String randomToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String currentIssuer() {
        return issuer;
    }

    private static boolean isUnsafePlaceholder(String value) {
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.equals("dev-secret-change-this-value-please")
                || normalized.equals("dev-only-change-me")
                || normalized.contains("dev-only-change-me")
                || normalized.contains("change-this")
                || normalized.contains("changeme")
                || normalized.equals("password")
                || normalized.equals("secret");
    }

    private static String generatedTestSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
