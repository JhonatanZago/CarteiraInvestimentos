package com.example.carteirainvestimento.api;

import com.example.carteirainvestimento.dto.admin.AdminCreateUserRequest;
import com.example.carteirainvestimento.dto.admin.AdminPasswordRequest;
import com.example.carteirainvestimento.dto.admin.AdminStatusRequest;
import com.example.carteirainvestimento.dto.admin.AdminUpdateUserRequest;
import com.example.carteirainvestimento.dto.admin.UsuarioAdminResponse;
import com.example.carteirainvestimento.service.UsuarioAdministracaoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
public class UsuarioAdministracaoController {
    private final UsuarioAdministracaoService service;

    public UsuarioAdministracaoController(UsuarioAdministracaoService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UsuarioAdminResponse> listar() { return service.listar(); }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioAdminResponse buscar(@PathVariable Long id) { return service.buscar(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioAdminResponse criar(@Valid @RequestBody AdminCreateUserRequest request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioAdminResponse atualizar(@PathVariable Long id, @Valid @RequestBody AdminUpdateUserRequest request) {
        return service.atualizar(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioAdminResponse atualizarStatus(@PathVariable Long id, @Valid @RequestBody AdminStatusRequest request) {
        return service.atualizarStatus(id, request);
    }

    @PutMapping("/{id}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void alterarSenha(@PathVariable Long id, @Valid @RequestBody AdminPasswordRequest request) {
        service.alterarSenha(id, request);
    }
}
