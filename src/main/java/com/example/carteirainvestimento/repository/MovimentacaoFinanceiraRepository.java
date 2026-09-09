package com.example.carteirainvestimento.repository;

import com.example.carteirainvestimento.domain.MovimentacaoFinanceira;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoFinanceiraRepository extends JpaRepository<MovimentacaoFinanceira, Long> {
    List<MovimentacaoFinanceira> findByCarteiraIdOrderByDataOperacaoAscIdAsc(Long carteiraId);

    List<MovimentacaoFinanceira> findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscIdAsc(Long carteiraId, Long acaoId);

    boolean existsByAcaoId(Long acaoId);

    boolean existsByCorretoraId(Long corretoraId);

    List<MovimentacaoFinanceira> findByCarteiraIdAndDataOperacaoBetweenOrderByDataOperacaoAscIdAsc(
            Long carteiraId, LocalDate from, LocalDate to);
}
