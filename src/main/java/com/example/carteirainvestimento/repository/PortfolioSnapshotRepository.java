package com.example.carteirainvestimento.repository;

import com.example.carteirainvestimento.domain.PortfolioSnapshot;
import com.example.carteirainvestimento.enums.OrigemSnapshotCarteira;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {
    void deleteByCarteiraId(Long carteiraId);

    List<PortfolioSnapshot> findByCarteiraIdOrderByDataHoraAsc(Long carteiraId);
    Optional<PortfolioSnapshot> findTopByCarteiraIdOrderByDataHoraDesc(Long carteiraId);
    Optional<PortfolioSnapshot> findFirstByCarteiraIdAndOrigemAndDataHoraBetween(Long carteiraId, OrigemSnapshotCarteira origem, OffsetDateTime inicio, OffsetDateTime fim);
}
