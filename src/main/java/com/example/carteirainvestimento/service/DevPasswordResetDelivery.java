package com.example.carteirainvestimento.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Implementação segura para desenvolvimento: não registra o token nem o devolve na API. */
@Component
@Profile({"dev", "test"})
public class DevPasswordResetDelivery implements PasswordResetDelivery {
    @Override
    public void send(String email, String token) {
        // Integrações de e-mail podem substituir este adapter em produção.
    }
}
