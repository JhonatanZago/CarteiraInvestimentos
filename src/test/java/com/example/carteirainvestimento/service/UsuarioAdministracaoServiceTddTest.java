package com.example.carteirainvestimento.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.carteirainvestimento.domain.Usuario;
import com.example.carteirainvestimento.dto.admin.AdminCreateUserRequest;
import com.example.carteirainvestimento.dto.admin.AdminStatusRequest;
import com.example.carteirainvestimento.dto.admin.AdminUpdateUserRequest;
import com.example.carteirainvestimento.enums.PerfilUsuario;
import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.exception.DuplicateResourceException;
import com.example.carteirainvestimento.repository.RefreshTokenRepository;
import com.example.carteirainvestimento.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UsuarioAdministracaoServiceTddTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final UsuarioAdministracaoService service = new UsuarioAdministracaoService(usuarios, refreshTokens, encoder);

    @Test
    void rejectsDemotionOfTheLastActiveAdmin() {
        Usuario admin = user(7L, "admin@example.com", PerfilUsuario.ADMIN, true);
        when(usuarios.findById(7L)).thenReturn(Optional.of(admin));
        when(usuarios.countByPerfilAndAtivoTrue(PerfilUsuario.ADMIN)).thenReturn(1L);

        assertThrows(BusinessRuleException.class, () -> service.atualizar(7L,
                new AdminUpdateUserRequest("Admin", "admin@example.com", PerfilUsuario.USER)));
        verify(usuarios, never()).save(any());
    }

    @Test
    void rejectsDuplicateEmailWhenUpdatingUser() {
        Usuario user = user(8L, "old@example.com", PerfilUsuario.USER, true);
        when(usuarios.findById(8L)).thenReturn(Optional.of(user));
        when(usuarios.existsByEmailIgnoreCase("used@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.atualizar(8L,
                new AdminUpdateUserRequest("User", "used@example.com", PerfilUsuario.USER)));
        verify(usuarios, never()).save(any());
    }

    @Test
    void rejectsMismatchedPasswordBeforePersisting() {
        assertThrows(BusinessRuleException.class, () -> service.criar(
                new AdminCreateUserRequest("User", "new@example.com", "one", "two")));
        verify(usuarios, never()).save(any());
    }

    @Test
    void rejectsDeactivationOfTheLastActiveAdmin() {
        Usuario admin = user(9L, "admin2@example.com", PerfilUsuario.ADMIN, true);
        when(usuarios.findById(9L)).thenReturn(Optional.of(admin));
        when(usuarios.countByPerfilAndAtivoTrue(PerfilUsuario.ADMIN)).thenReturn(1L);

        assertThrows(BusinessRuleException.class, () -> service.atualizarStatus(9L, new AdminStatusRequest(false)));
        verify(usuarios, never()).save(any());
    }

    private Usuario user(Long id, String email, PerfilUsuario perfil, boolean ativo) {
        Usuario user = new Usuario();
        user.setId(id);
        user.setEmail(email);
        user.setNome("User");
        user.setPerfil(perfil);
        user.setAtivo(ativo);
        return user;
    }
}
