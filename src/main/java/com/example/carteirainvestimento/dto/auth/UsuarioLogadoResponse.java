package com.example.carteirainvestimento.dto.auth;

import com.example.carteirainvestimento.enums.PerfilUsuario;

public record UsuarioLogadoResponse(Long id, String nome, String email, PerfilUsuario perfil) { }
