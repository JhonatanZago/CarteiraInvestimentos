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

@Service
public class CotacaoRefreshService {
    private final AcaoRepository acoes; private final HistoricoCotacaoRepository historicos; private final CotacaoFacade cotacoes;
    public CotacaoRefreshService(AcaoRepository acoes, HistoricoCotacaoRepository historicos, CotacaoFacade cotacoes) { this.acoes = acoes; this.historicos = historicos; this.cotacoes = cotacoes; }
    @Transactional
    public Acao atualizar(Long id) {
        Acao acao = acoes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Acao nao encontrada"));
        CotacaoConsulta cotacao = cotacoes.buscarCotacao(acao.getTicker(), acao.getMercado());
        acao.setCotacaoAtual(cotacao.valor()); acao.setDataHoraCotacao(cotacao.dataHoraCotacao());
        if (!historicos.existsByAcaoIdAndDataHoraCotacao(id, cotacao.dataHoraCotacao())) {
            HistoricoCotacao h = new HistoricoCotacao(); h.setAcao(acao); h.setValor(cotacao.valor()); h.setDataHoraCotacao(cotacao.dataHoraCotacao()); h.setDataHoraRegistro(OffsetDateTime.now(ZoneOffset.UTC)); h.setFonte(cotacao.fonte()); historicos.save(h);
        }
        return acoes.save(acao);
    }
}
