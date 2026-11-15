package com.example.carteirainvestimento.config;

import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String jwtSecret,
        String jwtIssuer,
        boolean refreshCookieSecure,
        String refreshCookieSameSite,
        String refreshCookiePath) {

    public AuthProperties(String jwtSecret, boolean refreshCookieSecure, String refreshCookieSameSite,
            String refreshCookiePath) {
        this(jwtSecret, "carteira-investimento", refreshCookieSecure, refreshCookieSameSite, refreshCookiePath);
    }

    @ConstructorBinding
    public AuthProperties {
        jwtSecret = jwtSecret == null ? "" : jwtSecret;
        jwtIssuer = jwtIssuer == null || jwtIssuer.isBlank() ? "carteira-investimento" : jwtIssuer.trim();
        refreshCookieSameSite = refreshCookieSameSite == null
                ? "Lax"
                : refreshCookieSameSite.trim();
        refreshCookiePath = refreshCookiePath == null || refreshCookiePath.isBlank()
                ? "/api/v1/auth"
                : refreshCookiePath.trim();
        String normalizedSameSite = refreshCookieSameSite.toLowerCase(Locale.ROOT);
        if (!normalizedSameSite.equals("lax")
                && !normalizedSameSite.equals("strict")
                && !normalizedSameSite.equals("none")) {
            throw new IllegalStateException("app.auth.refresh-cookie-same-site deve ser Lax, Strict ou None");
        }
        if (!refreshCookiePath.startsWith("/")) {
            throw new IllegalStateException("app.auth.refresh-cookie-path deve iniciar com '/'");
        }
        if (normalizedSameSite.equals("none") && !refreshCookieSecure) {
            throw new IllegalStateException("SameSite=None exige refresh-cookie-secure=true");
        }
    }
}
