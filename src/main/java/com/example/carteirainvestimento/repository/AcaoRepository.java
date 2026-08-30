package com.example.carteirainvestimento.repository;

import com.example.carteirainvestimento.domain.Acao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AcaoRepository extends JpaRepository<Acao, Long> {
    boolean existsByTicker(String ticker);
    Optional<Acao> findByTicker(String ticker);
}
