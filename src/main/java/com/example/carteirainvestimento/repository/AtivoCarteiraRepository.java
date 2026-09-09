package com.example.carteirainvestimento.repository;

import com.example.carteirainvestimento.domain.AtivoCarteira;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AtivoCarteiraRepository extends JpaRepository<AtivoCarteira, Long> {
    @Query("select distinct a.carteira.id from AtivoCarteira a where a.acao.id = :acaoId")
    List<Long> findCarteiraIdsByAcaoId(Long acaoId);

    boolean existsByCarteiraId(Long carteiraId);

    boolean existsByAcaoId(Long acaoId);

    boolean existsByCorretoraId(Long corretoraId);

    boolean existsByCarteiraIdAndAcaoIdAndCorretoraId(Long carteiraId, Long acaoId, Long corretoraId);

    boolean existsByCarteiraIdAndAcaoIdAndCorretoraIdAndIdNot(
            Long carteiraId, Long acaoId, Long corretoraId, Long id);

    List<AtivoCarteira> findByCarteiraId(Long carteiraId);
}
