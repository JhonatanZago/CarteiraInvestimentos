package com.example.carteirainvestimento.service;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.domain.AtivoCarteira;
import com.example.carteirainvestimento.domain.Carteira;
import com.example.carteirainvestimento.domain.Corretora;
import com.example.carteirainvestimento.domain.MovimentacaoFinanceira;
import com.example.carteirainvestimento.dto.movimentacao.MovimentacaoFinanceiraRequest;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.enums.TipoMovimentacao;
import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.repository.AcaoRepository;
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.repository.CarteiraRepository;
import com.example.carteirainvestimento.repository.CorretoraRepository;
import com.example.carteirainvestimento.repository.MovimentacaoFinanceiraRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registra operações individuais sem alterar a projeção legada AtivoCarteira. */
@Service
public class MovimentacaoFinanceiraService {
    private final MovimentacaoFinanceiraRepository movimentacoes;
    private final CarteiraRepository carteiras;
    private final AcaoRepository acoes;
    private final CorretoraRepository corretoras;
    private final ExchangeRateService exchangeRates;

    public MovimentacaoFinanceiraService(MovimentacaoFinanceiraRepository movimentacoes,
            CarteiraRepository carteiras, AcaoRepository acoes, CorretoraRepository corretoras,
            ExchangeRateService exchangeRates) {
        this.movimentacoes = movimentacoes;
        this.carteiras = carteiras;
        this.acoes = acoes;
        this.corretoras = corretoras;
        this.exchangeRates = exchangeRates;
    }

    @Transactional
    public MovimentacaoFinanceira registrar(Long carteiraId, Long acaoId, Long corretoraId,
            MovimentacaoFinanceiraRequest request) {
        validar(request);
        Carteira carteira = carteiras.findById(carteiraId)
                .orElseThrow(() -> new ResourceNotFoundException("Carteira nao encontrada"));
        Acao acao = acoes.findById(acaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Acao nao encontrada"));
        Corretora corretora = corretoras.findById(corretoraId)
                .orElseThrow(() -> new ResourceNotFoundException("Corretora nao encontrada"));

        Moeda moeda = acao.getMoeda();
        BigDecimal cambio = moeda == Moeda.BRL ? BigDecimal.ONE : request.cambioParaBrl();
        if (moeda != Moeda.BRL && (cambio == null || cambio.signum() <= 0)) {
            cambio = exchangeRates.usdToBrl();
        }
        if (moeda != Moeda.BRL && (cambio == null || cambio.signum() <= 0)) {
            throw new BusinessRuleException("Cambio para BRL indisponivel para a operacao");
        }
        if (request.tipo() == TipoMovimentacao.VENDA) {
            BigDecimal saldo = saldo(carteiraId, acaoId);
            if (saldo.compareTo(request.quantidade()) < 0) {
                throw new BusinessRuleException("Quantidade vendida superior ao saldo disponivel");
            }
        }

        MovimentacaoFinanceira movimento = new MovimentacaoFinanceira();
        movimento.setCarteira(carteira);
        movimento.setAcao(acao);
        movimento.setCorretora(corretora);
        movimento.setTipo(request.tipo());
        movimento.setQuantidade(request.quantidade());
        movimento.setPrecoUnitario(request.precoUnitario());
        movimento.setMoeda(moeda);
        movimento.setCambioParaBrl(cambio);
        movimento.setCambioEm(request.cambioEm());
        movimento.setCambioFonte(request.cambioFonte());
        movimento.setValorOriginal(CalculadoraMovimentacao.valorOriginal(request.quantidade(), request.precoUnitario()));
        movimento.setValorBrl(CalculadoraMovimentacao.valorEmBrl(movimento.getValorOriginal(), cambio));
        movimento.setCustosBrl(request.custosBrl());
        movimento.setDataOperacao(request.dataOperacao());
        return movimentacoes.save(movimento);
    }

    @Transactional(readOnly = true)
    public BigDecimal saldo(Long carteiraId, Long acaoId) {
        return movimentacoes.findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscIdAsc(carteiraId, acaoId)
                .stream().reduce(BigDecimal.ZERO, (saldo, item) -> item.getTipo() == TipoMovimentacao.COMPRA
                        ? saldo.add(item.getQuantidade()) : saldo.subtract(item.getQuantidade()), BigDecimal::add);
    }

    /** Resultado realizado em moeda original, pelo custo médio ponderado das compras. */
    @Transactional(readOnly = true)
    public BigDecimal resultadoRealizado(Long carteiraId, Long acaoId) {
        BigDecimal saldo = BigDecimal.ZERO;
        BigDecimal custoTotal = BigDecimal.ZERO;
        BigDecimal resultado = BigDecimal.ZERO;
        for (MovimentacaoFinanceira item : movimentacoes
                .findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscIdAsc(carteiraId, acaoId)) {
            BigDecimal valor = item.getValorOriginal();
            if (item.getTipo() == TipoMovimentacao.COMPRA) {
                saldo = saldo.add(item.getQuantidade());
                custoTotal = custoTotal.add(valor);
            } else if (saldo.signum() > 0) {
                BigDecimal custoVenda = custoTotal.multiply(item.getQuantidade()).divide(saldo, 8,
                        java.math.RoundingMode.HALF_EVEN);
                resultado = resultado.add(CalculadoraMovimentacao.resultadoVenda(valor, custoVenda,
                        item.getCustosBrl() == null ? BigDecimal.ZERO : item.getCustosBrl()));
                saldo = saldo.subtract(item.getQuantidade());
                custoTotal = custoTotal.subtract(custoVenda);
            }
        }
        return resultado;
    }

    private void validar(MovimentacaoFinanceiraRequest request) {
        if (request == null || request.tipo() == null || request.quantidade() == null
                || request.quantidade().signum() <= 0 || request.precoUnitario() == null
                || request.precoUnitario().signum() <= 0 || request.dataOperacao() == null
                || request.dataOperacao().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Dados da movimentacao invalidos");
        }
    }
}
