package com.example.carteirainvestimento.service;

import com.example.carteirainvestimento.domain.*;
import com.example.carteirainvestimento.dto.venda.*;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.exception.*;
import com.example.carteirainvestimento.repository.*;
import java.math.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendaService {
    private static final int SCALE = 8;
    private final CarteiraRepository carteiras;
    private final AtivoCarteiraRepository ativos;
    private final VendaRepository vendas;

    public VendaService(CarteiraRepository carteiras, AtivoCarteiraRepository ativos, VendaRepository vendas) {
        this.carteiras = carteiras; this.ativos = ativos; this.vendas = vendas;
    }

    @Transactional
    public VendaResponse simular(Long carteiraId, VendaRequest request) {
        return calcular(carteiraId, request, buscarPosicao(carteiraId, request));
    }

    @Transactional
    public VendaResponse confirmar(Long carteiraId, VendaRequest request) {
        AtivoCarteira posicao = buscarPosicao(carteiraId, request);
        VendaResponse calculo = calcular(carteiraId, request, posicao);
        Venda venda = new Venda();
        venda.setCarteira(posicao.getCarteira()); venda.setAcao(posicao.getAcao()); venda.setCorretora(posicao.getCorretora()); venda.setPosicaoId(posicao.getId());
        venda.setQuantidade(request.quantidade()); venda.setPrecoMedio(posicao.getPrecoMedio()); venda.setPrecoVenda(request.precoVenda()); venda.setTaxas(request.taxas());
        venda.setValorBruto(calculo.valorBruto()); venda.setCustoPosicao(calculo.custoPosicao()); venda.setResultadoRealizado(calculo.resultadoRealizado()); venda.setMoeda(posicao.getAcao().getMoeda()); venda.setDataVenda(request.dataVenda());
        vendas.save(venda);
        if (calculo.quantidadeRestante().signum() == 0) ativos.delete(posicao);
        else { posicao.setQuantidade(calculo.quantidadeRestante()); ativos.save(posicao); }
        return new VendaResponse(venda.getId(), carteiraId, venda.getPosicaoId(), venda.getAcao().getTicker(), venda.getAcao().getNomeEmpresa(), venda.getAcao().getLogoUrl(),
                venda.getQuantidade(), calculo.quantidadeDisponivelAntes(), calculo.quantidadeRestante(), venda.getPrecoMedio(), venda.getPrecoVenda(), venda.getTaxas(), venda.getValorBruto(), venda.getCustoPosicao(), venda.getResultadoRealizado(), calculo.rentabilidadePercentual(), venda.getMoeda(), venda.getDataVenda());
    }

    @Transactional(readOnly = true)
    public List<VendaResponse> listar(Long carteiraId) {
        if (com.example.carteirainvestimento.security.CurrentUser.id().isPresent() ? carteiras.findByIdAndUsuarioId(carteiraId, com.example.carteirainvestimento.security.CurrentUser.id().get()).isEmpty() : !carteiras.existsById(carteiraId)) throw new ResourceNotFoundException("Carteira nao encontrada");
        return vendas.findByCarteiraIdOrderByDataVendaDescIdDesc(carteiraId).stream().map(v -> new VendaResponse(v.getId(), carteiraId, v.getPosicaoId(), v.getAcao().getTicker(), v.getAcao().getNomeEmpresa(), v.getAcao().getLogoUrl(), v.getQuantidade(), null, null, v.getPrecoMedio(), v.getPrecoVenda(), v.getTaxas(), v.getValorBruto(), v.getCustoPosicao(), v.getResultadoRealizado(), percentual(v.getResultadoRealizado(), v.getCustoPosicao()), v.getMoeda(), v.getDataVenda())).toList();
    }

    private AtivoCarteira buscarPosicao(Long carteiraId, VendaRequest request) {
        if (com.example.carteirainvestimento.security.CurrentUser.id().isPresent() ? carteiras.findByIdAndUsuarioId(carteiraId, com.example.carteirainvestimento.security.CurrentUser.id().get()).isEmpty() : !carteiras.existsById(carteiraId)) throw new ResourceNotFoundException("Carteira nao encontrada");
        if (request == null || request.posicaoId() == null) throw new BusinessRuleException("Posicao obrigatoria");
        return ativos.findByIdAndCarteiraId(request.posicaoId(), carteiraId).orElseThrow(() -> new ResourceNotFoundException("Posicao nao encontrada nesta carteira"));
    }
    private VendaResponse calcular(Long carteiraId, VendaRequest request, AtivoCarteira p) {
        if (request.dataVenda() == null || request.dataVenda().isAfter(LocalDate.now()) || request.dataVenda().isBefore(p.getDataPrimeiraCompra())) throw new BusinessRuleException("A data da venda deve estar entre a primeira compra e hoje");
        if (request.quantidade() == null || request.quantidade().signum() <= 0 || request.quantidade().compareTo(p.getQuantidade()) > 0) throw new BusinessRuleException("A quantidade informada ultrapassa o saldo disponível de " + p.getQuantidade() + " ações de " + p.getAcao().getTicker() + ".");
        if (request.precoVenda() == null || request.precoVenda().signum() <= 0) throw new BusinessRuleException("O preço de venda deve ser maior que zero");
        if (request.taxas() == null || request.taxas().signum() < 0) throw new BusinessRuleException("As taxas não podem ser negativas");
        Moeda moeda = p.getAcao().getMoeda(); if (request.moeda() != null && request.moeda() != moeda) throw new BusinessRuleException("A moeda informada não corresponde ao ativo");
        BigDecimal bruto = request.quantidade().multiply(request.precoVenda()).setScale(SCALE, RoundingMode.HALF_EVEN);
        BigDecimal custo = request.quantidade().multiply(p.getPrecoMedio()).setScale(SCALE, RoundingMode.HALF_EVEN);
        BigDecimal resultado = bruto.subtract(custo).subtract(request.taxas()).setScale(SCALE, RoundingMode.HALF_EVEN);
        return new VendaResponse(null, carteiraId, p.getId(), p.getAcao().getTicker(), p.getAcao().getNomeEmpresa(), p.getAcao().getLogoUrl(), request.quantidade(), p.getQuantidade(), p.getQuantidade().subtract(request.quantidade()), p.getPrecoMedio(), request.precoVenda(), request.taxas(), bruto, custo, resultado, percentual(resultado, custo), moeda, request.dataVenda());
    }
    private BigDecimal percentual(BigDecimal resultado, BigDecimal custo) { return custo.signum() == 0 ? BigDecimal.ZERO : resultado.multiply(BigDecimal.valueOf(100)).divide(custo, 4, RoundingMode.HALF_EVEN); }
}
