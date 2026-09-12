package com.example.carteirainvestimento.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthTokenService {
    private final byte[] secret;
    public AuthTokenService(@Value("${app.auth.jwt-secret:dev-only-change-me}") String secret) {
        if (secret.length() < 24) throw new IllegalStateException("app.auth.jwt-secret deve possuir ao menos 24 caracteres");
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
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
}
