package com.example.carteirainvestimento.service;

/** Abstração para entrega do link de recuperação sem acoplar o domínio a SMTP. */
public interface PasswordResetDelivery {
    void send(String email, String token);
}
