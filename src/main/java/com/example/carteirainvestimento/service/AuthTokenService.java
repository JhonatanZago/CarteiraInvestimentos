package com.example.carteirainvestimento.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.example.carteirainvestimento.config.AuthProperties;

@Service
public class AuthTokenService {
    private final byte[] secret;
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
        this.secret = configured.getBytes(StandardCharsets.UTF_8);
    }
    public String accessToken(Long userId, long ttlSeconds) {
        long exp = Instant.now().plusSeconds(ttlSeconds).getEpochSecond();
        String payload = userId + "." + exp;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + sign(payload);
    }
    public Long userId(String token) {
        try {
            String[] parts = token.split("\\.", 2);
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) return null;
            String[] values = payload.split("\\.");
            if (Long.parseLong(values[1]) <= Instant.now().getEpochSecond()) return null;
            return Long.valueOf(values[0]);
        } catch (RuntimeException ex) { return null; }
    }
    public String hash(String value) {
        try { return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
    public String randomToken() { return Base64.getUrlEncoder().withoutPadding().encodeToString(java.util.UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)); }
    private String sign(String value) {
        try { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret, "HmacSHA256")); return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    private static boolean isUnsafePlaceholder(String value) {
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.equals("dev-secret-change-this-value-please")
                || normalized.equals("dev-only-change-me")
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
