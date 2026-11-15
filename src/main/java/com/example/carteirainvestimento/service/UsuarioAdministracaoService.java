package com.example.carteirainvestimento.service;

import com.example.carteirainvestimento.domain.Usuario;
import com.example.carteirainvestimento.dto.admin.AdminCreateUserRequest;
import com.example.carteirainvestimento.dto.admin.AdminPasswordRequest;
import com.example.carteirainvestimento.dto.admin.AdminStatusRequest;
import com.example.carteirainvestimento.dto.admin.AdminUpdateUserRequest;
import com.example.carteirainvestimento.dto.admin.UsuarioAdminResponse;
import com.example.carteirainvestimento.enums.PerfilUsuario;
import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.exception.DuplicateResourceException;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.repository.RefreshTokenRepository;
import com.example.carteirainvestimento.repository.UsuarioRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioAdministracaoService {
    private final UsuarioRepository usuarios;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;

    public UsuarioAdministracaoService(UsuarioRepository usuarios, RefreshTokenRepository refreshTokens,
                                       PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<UsuarioAdminResponse> listar() {
        return usuarios.findAll().stream().map(UsuarioAdminResponse::from).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public UsuarioAdminResponse buscar(Long id) {
        return UsuarioAdminResponse.from(find(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UsuarioAdminResponse criar(AdminCreateUserRequest request) {
        if (!request.senha().equals(request.confirmacaoSenha())) {
            throw new BusinessRuleException("As senhas não conferem");
        }
        String email = normalize(request.email());
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("E-mail já cadastrado");
        }
        Usuario usuario = new Usuario();
        usuario.setNome(request.nome().trim());
        usuario.setEmail(email);
        usuario.setSenha(encoder.encode(request.senha()));
        usuario.setPerfil(PerfilUsuario.USER);
        usuario.setAtivo(true);
        usuario.setCriadoEm(now());
        usuario.setAtualizadoEm(now());
        return UsuarioAdminResponse.from(usuarios.save(usuario));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UsuarioAdminResponse atualizar(Long id, AdminUpdateUserRequest request) {
        Usuario usuario = find(id);
        String email = normalize(request.email());
        if (!email.equalsIgnoreCase(usuario.getEmail()) && usuarios.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("E-mail já cadastrado");
        }
        if (usuario.getPerfil() == PerfilUsuario.ADMIN && request.perfil() != PerfilUsuario.ADMIN
                && Boolean.TRUE.equals(usuario.getAtivo())) {
            ensureAnotherActiveAdminExists(usuario.getId());
        }
        usuario.setNome(request.nome().trim());
        usuario.setEmail(email);
        usuario.setPerfil(request.perfil());
        usuario.setAtualizadoEm(now());
        return UsuarioAdminResponse.from(usuarios.save(usuario));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UsuarioAdminResponse atualizarStatus(Long id, AdminStatusRequest request) {
        Usuario usuario = find(id);
        if (!request.ativo() && usuario.getPerfil() == PerfilUsuario.ADMIN
                && Boolean.TRUE.equals(usuario.getAtivo())) {
            ensureAnotherActiveAdminExists(usuario.getId());
        }
        usuario.setAtivo(request.ativo());
        usuario.setAtualizadoEm(now());
        return UsuarioAdminResponse.from(usuarios.save(usuario));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void alterarSenha(Long id, AdminPasswordRequest request) {
        if (!request.senha().equals(request.confirmacaoSenha())) {
            throw new BusinessRuleException("As senhas não conferem");
        }
        Usuario usuario = find(id);
        usuario.setSenha(encoder.encode(request.senha()));
        usuario.setAtualizadoEm(now());
        refreshTokens.deleteByUsuarioId(id);
        usuarios.save(usuario);
    }

    private Usuario find(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private void ensureAnotherActiveAdminExists(Long excludedId) {
        long activeAdmins = usuarios.countByPerfilAndAtivoTrue(PerfilUsuario.ADMIN);
        if (activeAdmins <= 1) {
            throw new BusinessRuleException("O sistema deve manter ao menos um administrador ativo");
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
