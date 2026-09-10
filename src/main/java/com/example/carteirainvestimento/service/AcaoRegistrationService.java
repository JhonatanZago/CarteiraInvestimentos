package com.example.carteirainvestimento.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.domain.HistoricoCotacao;
import com.example.carteirainvestimento.dto.acao.AcaoCreateRequest;
import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.exception.DuplicateResourceException;
import com.example.carteirainvestimento.exception.AssetCountryMismatchException;
import com.example.carteirainvestimento.exception.AssetInUseException;
import com.example.carteirainvestimento.exception.ExternalIntegrationException;
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.integration.dto.CotacaoConsulta;
import com.example.carteirainvestimento.integration.facade.CotacaoFacade;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.enums.CountryCode;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.repository.AcaoRepository;
import com.example.carteirainvestimento.repository.HistoricoCotacaoRepository;
import com.example.carteirainvestimento.repository.MovimentacaoFinanceiraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
public class AcaoRegistrationService {
    private final AcaoRepository acaoRepository;
    private final HistoricoCotacaoRepository historicoRepository;
    private final CotacaoFacade cotacaoFacade; private final AtivoCarteiraRepository ativos;
    private final MovimentacaoFinanceiraRepository movimentacoes;
    public AcaoRegistrationService(AcaoRepository acaoRepository, HistoricoCotacaoRepository historicoRepository, CotacaoFacade cotacaoFacade, AtivoCarteiraRepository ativos) {
        this(acaoRepository, historicoRepository, cotacaoFacade, ativos, null);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public AcaoRegistrationService(AcaoRepository acaoRepository, HistoricoCotacaoRepository historicoRepository, CotacaoFacade cotacaoFacade, AtivoCarteiraRepository ativos, MovimentacaoFinanceiraRepository movimentacoes) {
        this.acaoRepository = acaoRepository; this.historicoRepository = historicoRepository; this.cotacaoFacade = cotacaoFacade; this.ativos = ativos; this.movimentacoes = movimentacoes;
    }

    @Transactional
    public void excluir(Long id) {
        Acao acao = acaoRepository.findById(id).orElseThrow(() -> new com.example.carteirainvestimento.exception.ResourceNotFoundException("Acao nao encontrada"));
        if (!ativos.findCarteiraIdsByAcaoId(id).isEmpty()
                || (movimentacoes != null && movimentacoes.existsByAcaoId(id)))
            throw new AssetInUseException("Este ativo nao pode ser excluido porque esta sendo utilizado em uma carteira ou possui movimentacoes financeiras.");
        // Historico de cotacao é dado técnico do próprio ativo; remova-o explicitamente, sem cascade financeiro.
        historicoRepository.deleteByAcaoId(id);
        acaoRepository.delete(acao);
    }
    @Transactional
    public Acao registrar(AcaoCreateRequest request) {
        String ticker = request.ticker().trim().toUpperCase();
        CountryCode selectedCountry = request.countryCode();
        if (ticker.isBlank()) throw new BusinessRuleException("Ticker invalido");
        CotacaoConsulta cotacao;
        try {
            cotacao = cotacaoFacade.buscarCotacao(ticker, selectedCountry.mercado());
        } catch (ResourceNotFoundException notFound) {
            // A busca secundária serve apenas para explicar o país correto; nunca cadastra.
            try {
                CotacaoConsulta detected = cotacaoFacade.buscarCotacao(ticker, selectedCountry.outro().mercado());
                throw new AssetCountryMismatchException("O ticker " + ticker + " é negociado em "
                        + (detected.mercado() == com.example.carteirainvestimento.enums.Mercado.BRASIL ? "Brasil" : "Estados Unidos")
                        + ". Selecione " + (detected.mercado() == com.example.carteirainvestimento.enums.Mercado.BRASIL ? "Brasil." : "Estados Unidos."));
            } catch (ResourceNotFoundException ignored) {
                throw notFound;
            }
        } catch (ExternalIntegrationException integrationFailure) {
            try {
                CotacaoConsulta detected = cotacaoFacade.buscarCotacao(ticker, selectedCountry.outro().mercado());
                throw new com.example.carteirainvestimento.exception.AssetCountryMismatchException("O ticker " + ticker + " pertence ao mercado " + detected.listingCountryCode() + ".");
            } catch (ResourceNotFoundException | ExternalIntegrationException ignored) {
                throw integrationFailure;
            }
        }
        if (cotacao == null) throw new ExternalIntegrationException("Fonte de cotacao nao retornou dados");
        String returnedTicker = cotacao.ticker() == null ? "" : cotacao.ticker().trim().toUpperCase();
        if (!ticker.equals(returnedTicker) || cotacao.valor() == null || cotacao.dataHoraCotacao() == null
                || cotacao.mercado() == null || cotacao.moeda() == null)
            throw new BusinessRuleException("Ativo nao encontrado ou resposta incompleta da fonte");
        if (cotacao.listingCountryCode() == null || !selectedCountry.name().equalsIgnoreCase(cotacao.listingCountryCode())
                || cotacao.mercado() != selectedCountry.mercado()
                || (selectedCountry == CountryCode.BR && cotacao.moeda() != Moeda.BRL)
                || (selectedCountry == CountryCode.US && cotacao.moeda() != Moeda.USD)) {
            String pais = cotacao.mercado() == com.example.carteirainvestimento.enums.Mercado.BRASIL ? "Brasil" : "Estados Unidos";
            String moeda = cotacao.moeda() == null ? "moeda não informada" : cotacao.moeda().name();
            throw new AssetCountryMismatchException("O ativo " + ticker + " é negociado em " + pais + " em " + moeda + ". Selecione o mercado correto.");
        }
        if (acaoRepository.existsByTicker(ticker)) throw new DuplicateResourceException("Ticker ja cadastrado");
        Acao acao = new Acao(); acao.setTicker(ticker); acao.setNomeEmpresa(cotacao.nomeEmpresa()); acao.setMercado(cotacao.mercado()); acao.setMoeda(cotacao.moeda()); acao.setCotacaoAtual(cotacao.valor()); acao.setDataHoraCotacao(cotacao.dataHoraCotacao()); acao.setLogoUrl(cotacao.logoUrl() != null ? cotacao.logoUrl() : AcaoLogoRegistry.logoFor(ticker));
        acao.setListingCountryCode(cotacao.listingCountryCode()); acao.setExchange(cotacao.exchange()); acao.setExchangeMic(cotacao.exchangeMic()); acao.setDataSource(cotacao.fonte() == null ? null : cotacao.fonte().name());
        acao = acaoRepository.save(acao);
        if (historicoRepository.existsByAcaoIdAndDataHoraCotacao(acao.getId(), cotacao.dataHoraCotacao())) {
            throw new DuplicateResourceException("Ponto historico de cotacao ja registrado");
        }
        HistoricoCotacao historico = new HistoricoCotacao(); historico.setAcao(acao); historico.setValor(cotacao.valor()); historico.setDataHoraCotacao(cotacao.dataHoraCotacao()); historico.setDataHoraRegistro(OffsetDateTime.now(ZoneOffset.UTC)); historico.setFonte(cotacao.fonte());
        historicoRepository.save(historico); return acao;
    }

    @Transactional
    public Acao revalidar(Long id) {
        Acao acao = acaoRepository.findById(id).orElseThrow(() -> new com.example.carteirainvestimento.exception.ResourceNotFoundException("Acao nao encontrada"));
        CotacaoConsulta cotacao = cotacaoFacade.buscarCotacao(acao.getTicker(), acao.getMercado());
        if (cotacao == null || cotacao.valor() == null || cotacao.dataHoraCotacao() == null
                || cotacao.mercado() == null || cotacao.moeda() == null || cotacao.listingCountryCode() == null)
            throw new BusinessRuleException("Fonte nao retornou metadados completos para " + acao.getTicker());
        acao.setMercado(cotacao.mercado()); acao.setMoeda(cotacao.moeda()); acao.setCotacaoAtual(cotacao.valor());
        acao.setDataHoraCotacao(cotacao.dataHoraCotacao()); acao.setListingCountryCode(cotacao.listingCountryCode());
        acao.setExchange(cotacao.exchange()); acao.setExchangeMic(cotacao.exchangeMic());
        acao.setDataSource(cotacao.fonte() == null ? null : cotacao.fonte().name());
        if (cotacao.logoUrl() != null) acao.setLogoUrl(cotacao.logoUrl());
        else if (acao.getLogoUrl() == null) acao.setLogoUrl(AcaoLogoRegistry.logoFor(acao.getTicker()));
        return acaoRepository.save(acao);
    }

    @Transactional
    public com.example.carteirainvestimento.dto.acao.RevalidacaoLoteResponse revalidarTodos() {
        List<String> erros = new ArrayList<>(); List<com.example.carteirainvestimento.dto.acao.RevalidacaoDetalhe> detalhes = new ArrayList<>(); int atualizados = 0; int semLogo = 0;
        List<Acao> existentes = acaoRepository.findAll();
        for (Acao acao : existentes) {
            var moedaAnterior = acao.getMoeda(); var mercadoAnterior = acao.getMercado(); var logoAnterior = acao.getLogoUrl();
            try {
                Acao atualizada = revalidar(acao.getId());
                atualizados++;
                if (atualizada.getLogoUrl() == null) semLogo++;
                detalhes.add(new com.example.carteirainvestimento.dto.acao.RevalidacaoDetalhe(acao.getId(), acao.getTicker(),
                        moedaAnterior != atualizada.getMoeda() || mercadoAnterior != atualizada.getMercado() || !java.util.Objects.equals(logoAnterior, atualizada.getLogoUrl()),
                        moedaAnterior, atualizada.getMoeda(), mercadoAnterior, atualizada.getMercado(), logoAnterior, atualizada.getLogoUrl(), null));
            } catch (RuntimeException ex) {
                erros.add(acao.getTicker() + ": " + (ex.getMessage() == null ? "falha na fonte" : ex.getMessage()));
                detalhes.add(new com.example.carteirainvestimento.dto.acao.RevalidacaoDetalhe(acao.getId(), acao.getTicker(), false,
                        moedaAnterior, moedaAnterior, mercadoAnterior, mercadoAnterior, logoAnterior, logoAnterior, ex.getMessage()));
            }
        }
        return new com.example.carteirainvestimento.dto.acao.RevalidacaoLoteResponse(existentes.size(), atualizados, semLogo, List.copyOf(erros), List.copyOf(detalhes));
    }
}
