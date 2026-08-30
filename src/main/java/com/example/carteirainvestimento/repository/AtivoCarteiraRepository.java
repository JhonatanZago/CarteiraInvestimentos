package com.example.carteirainvestimento.repository;

import com.example.carteirainvestimento.domain.AtivoCarteira;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtivoCarteiraRepository extends JpaRepository<AtivoCarteira, Long> {

    boolean existsByCarteiraId(Long carteiraId);

    boolean existsByCarteiraIdAndAcaoIdAndCorretoraId(Long carteiraId, Long acaoId, Long corretoraId);

    boolean existsByCarteiraIdAndAcaoIdAndCorretoraIdAndIdNot(
            Long carteiraId, Long acaoId, Long corretoraId, Long id);

    List<AtivoCarteira> findByCarteiraId(Long carteiraId);
}
