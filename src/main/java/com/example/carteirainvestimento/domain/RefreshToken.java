package com.example.carteirainvestimento.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;

@Entity @Table(name="refresh_token", indexes=@Index(name="ix_refresh_token_hash", columnList="tokenHash", unique=true))
@Getter @Setter @NoArgsConstructor
public class RefreshToken {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) private Usuario usuario;
    @Column(nullable=false, unique=true) private String tokenHash;
    @Column(nullable=false) private OffsetDateTime expiraEm;
    @Column(nullable=false) private boolean usado;
}
