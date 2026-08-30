package com.example.carteirainvestimento.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.domain.HistoricoCotacao;
import com.example.carteirainvestimento.dto.acao.AcaoCreateRequest;
import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.exception.DuplicateResourceException;
import com.example.carteirainvestimento.integration.dto.CotacaoConsulta;
import com.example.carteirainvestimento.integration.facade.CotacaoFacade;
import com.example.carteirainvestimento.repository.AcaoRepository;
import com.example.carteirainvestimento.repository.HistoricoCotacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcaoRegistrationService {
    private final AcaoRepository acaoRepository;
    private final HistoricoCotacaoRepository historicoRepository;
    private final CotacaoFacade cotacaoFacade;
    public AcaoRegistrationService(AcaoRepository acaoRepository, HistoricoCotacaoRepository historicoRepository, CotacaoFacade cotacaoFacade) {
        this.acaoRepository = acaoRepository; this.historicoRepository = historicoRepository; this.cotacaoFacade = cotacaoFacade;
    }
    @Transactional
    public Acao registrar(AcaoCreateRequest request) {
        String ticker = request.ticker().trim().toUpperCase();
        if (ticker.isBlank()) throw new BusinessRuleException("Ticker invalido");
        if (acaoRepository.existsByTicker(ticker)) throw new DuplicateResourceException("Ticker ja cadastrado");
        CotacaoConsulta cotacao = cotacaoFacade.buscarCotacao(ticker, request.mercado());
        if (cotacao.mercado() != request.mercado() || !ticker.equals(cotacao.ticker()) || cotacao.valor() == null || cotacao.dataHoraCotacao() == null) throw new BusinessRuleException("Cotacao invalida para o mercado informado");
        Acao acao = new Acao(); acao.setTicker(ticker); acao.setNomeEmpresa(cotacao.nomeEmpresa()); acao.setMercado(cotacao.mercado()); acao.setMoeda(cotacao.moeda()); acao.setCotacaoAtual(cotacao.valor()); acao.setDataHoraCotacao(cotacao.dataHoraCotacao());
        acao = acaoRepository.save(acao);
        if (historicoRepository.existsByAcaoIdAndDataHoraCotacao(acao.getId(), cotacao.dataHoraCotacao())) {
            throw new DuplicateResourceException("Ponto historico de cotacao ja registrado");
        }
        HistoricoCotacao historico = new HistoricoCotacao(); historico.setAcao(acao); historico.setValor(cotacao.valor()); historico.setDataHoraCotacao(cotacao.dataHoraCotacao()); historico.setDataHoraRegistro(OffsetDateTime.now(ZoneOffset.UTC)); historico.setFonte(cotacao.fonte());
        historicoRepository.save(historico); return acao;
    }
}
