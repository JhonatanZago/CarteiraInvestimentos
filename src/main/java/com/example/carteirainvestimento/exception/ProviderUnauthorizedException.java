package com.example.carteirainvestimento.exception;

/** Credencial ausente, inválida ou não ativada no provedor externo. */
public class ProviderUnauthorizedException extends ApplicationException {
    public ProviderUnauthorizedException(String message) { super(ErrorCode.PROVIDER_UNAUTHORIZED, message); }
}
