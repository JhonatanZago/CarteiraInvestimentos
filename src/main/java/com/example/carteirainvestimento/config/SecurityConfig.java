package com.example.carteirainvestimento.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.carteirainvestimento.service.AuthService;
import com.example.carteirainvestimento.service.AuthTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, AuthTokenService tokens, AuthService auth,
                                 Environment environment,
                                 @Value("${app.security.test-permit-all:false}") boolean testPermitAll) {
        boolean local = environment.acceptsProfiles(Profiles.of("local"));
        http.csrf(c -> c.disable())
                .cors(c -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> {
                    var publicAuth = a.requestMatchers(
                            "/api/v1/auth/cadastro", "/api/v1/auth/login", "/api/v1/auth/refresh",
                            "/api/v1/auth/logout", "/api/v1/auth/esqueci-minha-senha",
                            "/api/v1/auth/redefinir-senha", "/api-docs/**", "/swagger-ui/**", "/error"
                    ).permitAll();
                    if (local) {
                        publicAuth.requestMatchers("/h2-console/**").permitAll();
                    }
                    if (testPermitAll && environment.acceptsProfiles(Profiles.of("test"))) {
                        publicAuth.anyRequest().permitAll();
                    } else {
                        publicAuth.anyRequest().authenticated();
                    }
                })
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                jsonError(res, 401, "AUTHENTICATION_REQUIRED", "Autenticação necessária"))
                        .accessDeniedHandler((req, res, ex) ->
                                jsonError(res, 403, "FORBIDDEN", "Acesso não autorizado")))
                .headers(h -> h.frameOptions(f -> {
                    if (local) {
                        f.sameOrigin();
                    } else {
                        f.deny();
                    }
                })
                        .addHeaderWriter(new StaticHeadersWriter(
                                "Content-Security-Policy",
                                "default-src 'self'; script-src 'self' 'unsafe-inline'; "
                                        + "style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; "
                                        + "frame-ancestors 'none'; form-action 'self'"))
                        .addHeaderWriter(new StaticHeadersWriter(
                                "Permissions-Policy",
                                "camera=(), microphone=(), geolocation=(), payment=()"))
                        .addHeaderWriter(new StaticHeadersWriter(
                                "Cross-Origin-Opener-Policy", "same-origin"))
                        .addHeaderWriter(new StaticHeadersWriter(
                                "Cross-Origin-Embedder-Policy", "unsafe-none"))
                        .addHeaderWriter(new StaticHeadersWriter(
                                "Cross-Origin-Resource-Policy", "same-site")))
                .addFilterBefore(new AccessTokenFilter(tokens, auth), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void jsonError(HttpServletResponse response, int status,
                                  String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(OBJECT_MAPPER.writeValueAsBytes(
                Map.of("status", status, "code", code, "message", message)));
    }

    static final class AccessTokenFilter extends OncePerRequestFilter {
        private final AuthTokenService tokens;
        private final AuthService auth;

        AccessTokenFilter(AuthTokenService tokens, AuthService auth) {
            this.tokens = tokens;
            this.auth = auth;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            String header = req.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                Long id = tokens.userId(header.substring(7));
                if (id != null) {
                    try {
                        var user = auth.me(id);
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                        id, null,
                                        java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getPerfil()))));
                    } catch (com.example.carteirainvestimento.exception.ResourceNotFoundException ignored) {
                        SecurityContextHolder.clearContext();
                    }
                }
            }
            chain.doFilter(req, res);
        }
    }
}
