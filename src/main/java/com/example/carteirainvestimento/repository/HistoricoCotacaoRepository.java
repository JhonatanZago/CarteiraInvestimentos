package com.example.carteirainvestimento.repository;

import com.example.carteirainvestimento.domain.HistoricoCotacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface HistoricoCotacaoRepository extends JpaRepository<HistoricoCotacao, Long> {
    boolean existsByAcaoIdAndDataHoraCotacao(Long acaoId, OffsetDateTime dataHoraCotacao);
    List<HistoricoCotacao> findByAcaoIdOrderByDataHoraCotacaoDesc(Long acaoId);
}
