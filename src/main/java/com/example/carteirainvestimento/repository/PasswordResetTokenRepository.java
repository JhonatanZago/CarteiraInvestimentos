package com.example.carteirainvestimento.repository;
import com.example.carteirainvestimento.domain.PasswordResetToken; import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository;
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken,Long>{ Optional<PasswordResetToken> findByTokenHash(String hash); }
