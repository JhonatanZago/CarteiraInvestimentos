package com.example.carteirainvestimento.service;

import com.example.carteirainvestimento.domain.*;
import com.example.carteirainvestimento.dto.financeiro.ContextoMonetario;
import com.example.carteirainvestimento.dto.financeiro.DisponibilidadeMonetaria;
import com.example.carteirainvestimento.enums.Moeda;
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
    private final ExchangeRateService exchangeRates;
    public PortfolioSnapshotService(CarteiraRepository carteiras, AtivoCarteiraRepository ativos, PortfolioSnapshotRepository snapshots) {
        this(carteiras, ativos, snapshots, null);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public PortfolioSnapshotService(CarteiraRepository carteiras, AtivoCarteiraRepository ativos,
            PortfolioSnapshotRepository snapshots, ExchangeRateService exchangeRates) {
        this.carteiras = carteiras; this.ativos = ativos; this.snapshots = snapshots; this.exchangeRates = exchangeRates;
    }

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
        snapshot.setDataHora(momento); snapshot.setTotalInvestido(investido.setScale(4, RoundingMode.HALF_UP)); snapshot.setPatrimonioAtual(atual.setScale(4, RoundingMode.HALF_UP)); snapshot.setResultado(resultado.setScale(4, RoundingMode.HALF_UP)); snapshot.setRentabilidade(rentabilidade);
        preencherContextoBase(snapshot, posicoes);
        snapshots.save(snapshot);
    }

    private void preencherContextoBase(PortfolioSnapshot snapshot, List<AtivoCarteira> posicoes) {
        snapshot.setMoedaBase(Moeda.BRL);
        java.util.Map<Moeda, ContextoMonetario> contextos = new java.util.EnumMap<>(Moeda.class);
        boolean disponivel = true;
        BigDecimal investidoBase = BigDecimal.ZERO;
        BigDecimal atualBase = BigDecimal.ZERO;
        for (AtivoCarteira posicao : posicoes) {
            Moeda moeda = posicao.getAcao().getMoeda();
            if (moeda == null) {
                disponivel = false;
                continue;
            }
            ContextoMonetario contexto = contextos.computeIfAbsent(moeda, this::obterContexto);
            if (contexto.disponibilidade() != DisponibilidadeMonetaria.AVAILABLE
                    || contexto.taxaCambio() == null || contexto.taxaCambio().signum() <= 0) {
                disponivel = false;
                continue;
            }
            BigDecimal investido = posicao.getQuantidade().multiply(posicao.getPrecoMedio());
            BigDecimal cotacao = posicao.getAcao().getCotacaoAtual();
            if (cotacao == null) {
                disponivel = false;
                continue;
            }
            atualBase = atualBase.add(cotacao.multiply(posicao.getQuantidade()).multiply(contexto.taxaCambio()));
            investidoBase = investidoBase.add(investido.multiply(contexto.taxaCambio()));
        }
        snapshot.setDisponibilidadeMonetaria(disponivel ? DisponibilidadeMonetaria.AVAILABLE : DisponibilidadeMonetaria.UNAVAILABLE);
        if (disponivel) {
            BigDecimal resultadoBase = atualBase.subtract(investidoBase);
            BigDecimal rentabilidadeBase = investidoBase.signum() > 0
                    ? resultadoBase.multiply(BigDecimal.valueOf(100)).divide(investidoBase, 6, RoundingMode.HALF_UP) : null;
            snapshot.setTotalInvestidoBase(investidoBase.setScale(8, RoundingMode.HALF_UP));
            snapshot.setPatrimonioAtualBase(atualBase.setScale(8, RoundingMode.HALF_UP));
            snapshot.setResultadoBase(resultadoBase.setScale(8, RoundingMode.HALF_UP));
            snapshot.setRentabilidadeBase(rentabilidadeBase);
        } else {
            snapshot.setTotalInvestidoBase(null); snapshot.setPatrimonioAtualBase(null);
            snapshot.setResultadoBase(null); snapshot.setRentabilidadeBase(null);
        }
        List<PortfolioSnapshotCambio> cambios = contextos.values().stream().map(this::paraCambio).toList();
        snapshot.substituirCambios(cambios);
    }

    private ContextoMonetario obterContexto(Moeda moeda) {
        if (moeda == null) return ContextoMonetario.unavailable(null, Moeda.BRL, null, "FX_CURRENCY_UNRESOLVED");
        if (exchangeRates == null) {
            return moeda == Moeda.BRL
                    ? ContextoMonetario.available(Moeda.BRL, Moeda.BRL, BigDecimal.ONE, BigDecimal.ONE,
                            BigDecimal.ONE, OffsetDateTime.now(ZoneOffset.UTC), "IDENTITY")
                    : ContextoMonetario.unavailable(moeda, Moeda.BRL, null, "FX_PROVIDER_UNAVAILABLE");
        }
        return exchangeRates.obterTaxa(moeda, Moeda.BRL);
    }

    private PortfolioSnapshotCambio paraCambio(ContextoMonetario contexto) {
        PortfolioSnapshotCambio cambio = new PortfolioSnapshotCambio();
        cambio.setMoedaOriginal(contexto.moedaOriginal());
        cambio.setMoedaBase(contexto.moedaBase());
        cambio.setTaxaCambio(contexto.taxaCambio());
        cambio.setCambioEfetivoEm(contexto.cambioEfetivoEm());
        cambio.setFonteCambio(contexto.fonteCambio());
        cambio.setDisponibilidade(contexto.disponibilidade());
        return cambio;
    }
}
