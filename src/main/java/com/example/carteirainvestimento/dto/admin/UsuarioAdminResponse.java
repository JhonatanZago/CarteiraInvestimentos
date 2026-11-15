package com.example.carteirainvestimento.dto.admin;

import com.example.carteirainvestimento.domain.Usuario;
import com.example.carteirainvestimento.enums.PerfilUsuario;

public record UsuarioAdminResponse(Long id, String nome, String email, PerfilUsuario perfil, boolean ativo) {
    public static UsuarioAdminResponse from(Usuario usuario) {
        return new UsuarioAdminResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getPerfil(), Boolean.TRUE.equals(usuario.getAtivo()));
    }
}
