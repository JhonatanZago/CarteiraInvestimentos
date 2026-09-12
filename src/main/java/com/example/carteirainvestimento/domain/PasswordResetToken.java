package com.example.carteirainvestimento.domain;
import jakarta.persistence.*; import java.time.OffsetDateTime; import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
@Entity @Table(name="password_reset_token", indexes=@Index(name="ix_password_reset_hash", columnList="tokenHash", unique=true)) @Getter @Setter @NoArgsConstructor
public class PasswordResetToken { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @ManyToOne(optional=false,fetch=FetchType.LAZY) Usuario usuario; @Column(nullable=false,unique=true) String tokenHash; @Column(nullable=false) OffsetDateTime expiraEm; @Column(nullable=false) boolean usado; }
