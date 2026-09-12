package com.example.carteirainvestimento.domain;

import com.example.carteirainvestimento.enums.PerfilUsuario;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario", uniqueConstraints = @UniqueConstraint(name = "uk_usuario_email", columnNames = "email"))
@Getter @Setter @NoArgsConstructor
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String nome;
    @Column(nullable = false, unique = true) private String email;
    @Column(nullable = false) private String senha;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PerfilUsuario perfil = PerfilUsuario.USER;
    @Column(nullable = false) private Boolean ativo = true;
    @Column(nullable = false) private OffsetDateTime criadoEm;
    @Column(nullable = false) private OffsetDateTime atualizadoEm;
}
