package com.example.carteirainvestimento.service;

import com.example.carteirainvestimento.domain.RefreshToken;
import com.example.carteirainvestimento.domain.Usuario;
import com.example.carteirainvestimento.dto.auth.*;
import com.example.carteirainvestimento.enums.PerfilUsuario;
import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.exception.DuplicateResourceException;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.repository.RefreshTokenRepository;
import com.example.carteirainvestimento.repository.PasswordResetTokenRepository;
import com.example.carteirainvestimento.repository.UsuarioRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    public static final long ACCESS_TTL = 900;
    private final UsuarioRepository usuarios; private final RefreshTokenRepository refreshTokens; private final PasswordResetTokenRepository resetTokens; private final PasswordEncoder encoder; private final AuthTokenService tokens; private final PasswordResetDelivery delivery;
    @Autowired public AuthService(UsuarioRepository usuarios, RefreshTokenRepository refreshTokens, PasswordResetTokenRepository resetTokens, PasswordEncoder encoder, AuthTokenService tokens, org.springframework.beans.factory.ObjectProvider<PasswordResetDelivery> deliveries) { this.usuarios=usuarios; this.refreshTokens=refreshTokens; this.resetTokens=resetTokens; this.encoder=encoder; this.tokens=tokens; this.delivery=deliveries.getIfAvailable(() -> (email, token) -> {}); }
    @Transactional public Usuario cadastrar(CadastroUsuarioRequest r) {
        if (!r.senha().equals(r.confirmacaoSenha())) throw new BusinessRuleException("As senhas não conferem");
        String email = normalize(r.email()); if (usuarios.existsByEmailIgnoreCase(email)) throw new DuplicateResourceException("E-mail já cadastrado");
        Usuario u=new Usuario(); u.setNome(r.nome().trim()); u.setEmail(email); u.setSenha(encoder.encode(r.senha())); u.setPerfil(PerfilUsuario.USER); u.setAtivo(true); u.setCriadoEm(now()); u.setAtualizadoEm(now()); return usuarios.save(u);
    }
    @Transactional public LoginResult login(LoginRequest r) {
        Usuario u=usuarios.findByEmailIgnoreCase(normalize(r.email())).orElse(null);
        if (u==null || !u.getAtivo() || !encoder.matches(r.senha(),u.getSenha())) throw new BusinessRuleException("E-mail ou senha inválidos");
        long ttl=r.manterConectado()?2592000:86400; String raw=tokens.randomToken(); RefreshToken rt=new RefreshToken(); rt.setUsuario(u); rt.setTokenHash(tokens.hash(raw)); rt.setExpiraEm(now().plusSeconds(ttl)); rt.setUsado(false); refreshTokens.save(rt);
        return new LoginResult(new LoginResponse(tokens.accessToken(u.getId(), ACCESS_TTL), ACCESS_TTL, view(u)), raw, ttl);
    }
    @Transactional public LoginResult refresh(String raw) {
        if (raw==null) throw new BusinessRuleException("Sessão expirada"); RefreshToken rt=refreshTokens.findByTokenHash(tokens.hash(raw)).orElseThrow(()->new BusinessRuleException("Sessão expirada"));
        if (rt.isUsado() || rt.getExpiraEm().isBefore(now())) throw new BusinessRuleException("Sessão expirada"); rt.setUsado(true); String next=tokens.randomToken(); RefreshToken replacement=new RefreshToken(); replacement.setUsuario(rt.getUsuario()); replacement.setTokenHash(tokens.hash(next)); replacement.setExpiraEm(rt.getExpiraEm()); replacement.setUsado(false); refreshTokens.save(replacement); return new LoginResult(new LoginResponse(tokens.accessToken(rt.getUsuario().getId(),ACCESS_TTL),ACCESS_TTL,view(rt.getUsuario())), next, Math.max(1,java.time.Duration.between(now(),rt.getExpiraEm()).getSeconds()));
    }
    @Transactional public void logout(String raw){ if(raw!=null) refreshTokens.findByTokenHash(tokens.hash(raw)).ifPresent(refreshTokens::delete); }
    @Transactional public void solicitarReset(String email){ usuarios.findByEmailIgnoreCase(normalize(email)).ifPresent(u->{String raw=tokens.randomToken(); var reset=new com.example.carteirainvestimento.domain.PasswordResetToken(); reset.setUsuario(u); reset.setTokenHash(tokens.hash(raw)); reset.setExpiraEm(now().plusMinutes(30)); reset.setUsado(false); resetTokens.save(reset); delivery.send(u.getEmail(), raw);}); }
    @Transactional public void redefinir(RedefinirSenhaRequest request){ if(!request.senha().equals(request.confirmacaoSenha())) throw new BusinessRuleException("As senhas não conferem"); var reset=resetTokens.findByTokenHash(tokens.hash(request.token())).orElseThrow(()->new BusinessRuleException("Token de recuperação inválido")); if(reset.isUsado()||reset.getExpiraEm().isBefore(now())) throw new BusinessRuleException("Token de recuperação expirado"); var u=reset.getUsuario(); u.setSenha(encoder.encode(request.senha()));u.setAtualizadoEm(now());reset.setUsado(true);refreshTokens.deleteByUsuarioId(u.getId());usuarios.save(u);resetTokens.save(reset); }
    @Transactional(readOnly=true) public Usuario me(Long id){ return usuarios.findById(id).filter(Usuario::getAtivo).orElseThrow(()->new ResourceNotFoundException("Usuário não encontrado")); }
    public UsuarioLogadoResponse view(Usuario u){return new UsuarioLogadoResponse(u.getId(),u.getNome(),u.getEmail(),u.getPerfil());}
    private String normalize(String email){return email.trim().toLowerCase(Locale.ROOT);} private OffsetDateTime now(){return OffsetDateTime.now(ZoneOffset.UTC);}
    public record LoginResult(LoginResponse response,String refreshToken,long refreshTtlSeconds){}
}
