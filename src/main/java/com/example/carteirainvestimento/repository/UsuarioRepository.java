package com.example.carteirainvestimento.repository;

import com.example.carteirainvestimento.domain.Usuario;
import com.example.carteirainvestimento.enums.PerfilUsuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    long countByPerfilAndAtivoTrue(PerfilUsuario perfil);
}
