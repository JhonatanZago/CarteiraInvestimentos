package com.example.carteirainvestimento.dto.auth;

public record LoginResponse(String accessToken, long expiresInSeconds, UsuarioLogadoResponse usuario) { }
