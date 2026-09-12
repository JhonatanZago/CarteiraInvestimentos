package com.example.carteirainvestimento.api;

import com.example.carteirainvestimento.dto.auth.*; import com.example.carteirainvestimento.service.AuthService; import jakarta.servlet.http.Cookie; import jakarta.servlet.http.HttpServletRequest; import jakarta.servlet.http.HttpServletResponse; import jakarta.validation.Valid; import org.springframework.http.HttpStatus; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth; public AuthController(AuthService auth){this.auth=auth;}
    @PostMapping("/cadastro") @ResponseStatus(HttpStatus.CREATED) public UsuarioLogadoResponse cadastrar(@Valid @RequestBody CadastroUsuarioRequest r){return auth.view(auth.cadastrar(r));}
    @PostMapping("/login") public LoginResponse login(@Valid @RequestBody LoginRequest r,HttpServletResponse response){var result=auth.login(r); setCookie(response,result.refreshToken(),result.refreshTtlSeconds()); return result.response();}
    @PostMapping("/refresh") public LoginResponse refresh(HttpServletRequest request,HttpServletResponse response){var result=auth.refresh(cookie(request)); setCookie(response,result.refreshToken(),result.refreshTtlSeconds()); return result.response();}
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) public void logout(HttpServletRequest request,HttpServletResponse response){auth.logout(cookie(request)); clearCookie(response);}
    @GetMapping("/me") public UsuarioLogadoResponse me(Authentication authentication){return auth.view(auth.me((Long)authentication.getPrincipal()));}
    @PostMapping("/esqueci-minha-senha") public java.util.Map<String,String> forgot(@Valid @RequestBody SolicitarRecuperacaoSenhaRequest r){auth.solicitarReset(r.email());return java.util.Map.of("message","Se o e-mail estiver cadastrado, você receberá as instruções para redefinir sua senha.");}
    @PostMapping("/redefinir-senha") @ResponseStatus(HttpStatus.NO_CONTENT) public void reset(@Valid @RequestBody RedefinirSenhaRequest r){auth.redefinir(r);}
    private String cookie(HttpServletRequest r){if(r.getCookies()==null)return null;for(Cookie c:r.getCookies())if("refresh_token".equals(c.getName()))return c.getValue();return null;}
    private void setCookie(HttpServletResponse r,String value,long ttl){Cookie c=new Cookie("refresh_token",value);c.setHttpOnly(true);c.setSecure(false);c.setPath("/api/v1/auth");c.setMaxAge((int)Math.min(Integer.MAX_VALUE,ttl));r.addCookie(c);}
    private void clearCookie(HttpServletResponse r){Cookie c=new Cookie("refresh_token","");c.setHttpOnly(true);c.setPath("/api/v1/auth");c.setMaxAge(0);r.addCookie(c);}
}
