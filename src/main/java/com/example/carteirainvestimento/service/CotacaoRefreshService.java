package com.example.carteirainvestimento.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.domain.HistoricoCotacao;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.integration.dto.CotacaoConsulta;
import com.example.carteirainvestimento.integration.facade.CotacaoFacade;
import com.example.carteirainvestimento.repository.AcaoRepository;
import com.example.carteirainvestimento.repository.HistoricoCotacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.enums.OrigemSnapshotCarteira;

@Service
public class CotacaoRefreshService {
    private final AcaoRepository acoes; private final HistoricoCotacaoRepository historicos; private final CotacaoFacade cotacoes; private final AtivoCarteiraRepository ativos; private final PortfolioSnapshotService snapshots;
    public CotacaoRefreshService(AcaoRepository acoes, HistoricoCotacaoRepository historicos, CotacaoFacade cotacoes, AtivoCarteiraRepository ativos, PortfolioSnapshotService snapshots) { this.acoes = acoes; this.historicos = historicos; this.cotacoes = cotacoes; this.ativos = ativos; this.snapshots = snapshots; }
    @Transactional
    public Acao atualizar(Long id) {
        Acao acao = acoes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Acao nao encontrada"));
        CotacaoConsulta cotacao = cotacoes.buscarCotacao(acao.getTicker(), acao.getMercado());
        acao.setCotacaoAtual(cotacao.valor()); acao.setDataHoraCotacao(cotacao.dataHoraCotacao());
        acao.setMercado(cotacao.mercado()); acao.setMoeda(cotacao.moeda());
        acao.setListingCountryCode(cotacao.listingCountryCode()); acao.setExchange(cotacao.exchange());
        acao.setExchangeMic(cotacao.exchangeMic()); acao.setDataSource(cotacao.fonte() == null ? null : cotacao.fonte().name());
        if (cotacao.logoUrl() != null) acao.setLogoUrl(cotacao.logoUrl());
        if (!historicos.existsByAcaoIdAndDataHoraCotacao(id, cotacao.dataHoraCotacao())) {
            HistoricoCotacao h = new HistoricoCotacao(); h.setAcao(acao); h.setValor(cotacao.valor()); h.setDataHoraCotacao(cotacao.dataHoraCotacao()); h.setDataHoraRegistro(OffsetDateTime.now(ZoneOffset.UTC)); h.setFonte(cotacao.fonte()); historicos.save(h);
        }
        Acao salva = acoes.save(acao); ativos.findCarteiraIdsByAcaoId(id).forEach(carteiraId -> snapshots.registrarEvento(carteiraId, OrigemSnapshotCarteira.QUOTE_UPDATED)); return salva;
    }
}
