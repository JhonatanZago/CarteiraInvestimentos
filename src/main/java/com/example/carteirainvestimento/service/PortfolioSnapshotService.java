package com.example.carteirainvestimento.service;

import com.example.carteirainvestimento.domain.*;
import com.example.carteirainvestimento.enums.OrigemSnapshotCarteira;
import com.example.carteirainvestimento.repository.*;
import java.math.*;
import java.time.*;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioSnapshotService {
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private final CarteiraRepository carteiras; private final AtivoCarteiraRepository ativos; private final PortfolioSnapshotRepository snapshots;
    public PortfolioSnapshotService(CarteiraRepository carteiras, AtivoCarteiraRepository ativos, PortfolioSnapshotRepository snapshots) { this.carteiras = carteiras; this.ativos = ativos; this.snapshots = snapshots; }

    @Transactional
    public void registrarEvento(Long carteiraId, OrigemSnapshotCarteira origem) { registrar(carteiraId, origem, OffsetDateTime.now(SAO_PAULO), false); }

    @Scheduled(cron = "0 10 18 * * MON-FRI", zone = "America/Sao_Paulo")
    @Transactional
    public void registrarFechamentoDiario() { registrarFechamentoDiario(LocalDate.now(SAO_PAULO)); }

    /** Operação explícita e repetível para o fechamento de uma data local. */
    @Transactional
    public void registrarFechamentoDiario(LocalDate data) {
        OffsetDateTime momento = data.atTime(18, 10).atZone(SAO_PAULO).toOffsetDateTime();
        carteiras.findAll().forEach(carteira -> registrar(carteira.getId(), OrigemSnapshotCarteira.DAILY_CLOSE, momento, true));
    }

    private void registrar(Long carteiraId, OrigemSnapshotCarteira origem, OffsetDateTime momento, boolean diario) {
        Carteira carteira = carteiras.getReferenceById(carteiraId); List<AtivoCarteira> posicoes = ativos.findByCarteiraId(carteiraId);
        BigDecimal investido = posicoes.stream().map(p -> p.getQuantidade().multiply(p.getPrecoMedio())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal atual = posicoes.stream().map(p -> p.getQuantidade().multiply(p.getAcao().getCotacaoAtual())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal resultado = atual.subtract(investido); BigDecimal rentabilidade = investido.signum() > 0 ? resultado.multiply(BigDecimal.valueOf(100)).divide(investido, 6, RoundingMode.HALF_UP) : null;
        PortfolioSnapshot snapshot = diario ? snapshots.findFirstByCarteiraIdAndOrigemAndDataHoraBetween(carteiraId, origem, momento.toLocalDate().atStartOfDay(SAO_PAULO).toOffsetDateTime(), momento.toLocalDate().plusDays(1).atStartOfDay(SAO_PAULO).toOffsetDateTime().minusNanos(1)).orElse(null) : null;
        if (snapshot == null && !diario) {
            PortfolioSnapshot ultimo = snapshots.findTopByCarteiraIdOrderByDataHoraDesc(carteiraId).orElse(null);
            if (ultimo != null && ultimo.getTotalInvestido().compareTo(investido) == 0 && ultimo.getPatrimonioAtual().compareTo(atual) == 0) return;
        }
        if (snapshot == null) { snapshot = new PortfolioSnapshot(); snapshot.setCarteira(carteira); snapshot.setOrigem(origem); }
        snapshot.setDataHora(momento); snapshot.setTotalInvestido(investido.setScale(4, RoundingMode.HALF_UP)); snapshot.setPatrimonioAtual(atual.setScale(4, RoundingMode.HALF_UP)); snapshot.setResultado(resultado.setScale(4, RoundingMode.HALF_UP)); snapshot.setRentabilidade(rentabilidade); snapshots.save(snapshot);
    }
}
