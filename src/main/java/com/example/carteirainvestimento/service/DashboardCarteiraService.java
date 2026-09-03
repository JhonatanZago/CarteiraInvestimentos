package com.example.carteirainvestimento.service;

import com.example.carteirainvestimento.domain.AtivoCarteira;
import com.example.carteirainvestimento.dto.dashboard.ComposicaoCarteiraResponse;
import com.example.carteirainvestimento.dto.dashboard.DashboardCarteiraResponse;
import com.example.carteirainvestimento.dto.insight.ClassificacaoAlocacao;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.repository.CarteiraRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardCarteiraService {

    private final CarteiraRepository carteiras;
    private final AtivoCarteiraRepository ativos;

    public DashboardCarteiraService(CarteiraRepository carteiras, AtivoCarteiraRepository ativos) {
        this.carteiras = carteiras;
        this.ativos = ativos;
    }

    @Transactional(readOnly = true)
    public DashboardCarteiraResponse calcular(Long carteiraId) {
        if (!carteiras.existsById(carteiraId)) {
            throw new ResourceNotFoundException("Carteira nao encontrada");
        }

        List<AtivoCarteira> posicoes = ativos.findByCarteiraId(carteiraId);
        List<ComposicaoCarteiraResponse> composicao = posicoes.stream().map(this::composicao).toList();
        BigDecimal valorInvestido = composicao.stream()
                .map(ComposicaoCarteiraResponse::valorInvestido)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean cotacoesDisponiveis = composicao.stream().allMatch(item -> item.valorAtual() != null);
        BigDecimal valorAtual = cotacoesDisponiveis
                ? composicao.stream().map(ComposicaoCarteiraResponse::valorAtual).reduce(BigDecimal.ZERO, BigDecimal::add)
                : null;
        PosicaoFinanceira totais = composicao.isEmpty()
                ? new PosicaoFinanceira(BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), BigDecimal.ZERO)
                : CalculadoraFinanceira.calcular(BigDecimal.ONE, valorInvestido, valorAtual);
        OffsetDateTime ultimaAtualizacao = posicoes.stream()
                .map(posicao -> posicao.getAcao().getDataHoraCotacao())
                .filter(java.util.Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(null);

        return new DashboardCarteiraResponse(carteiraId,
                totais.valorInvestido(), totais.valorAtual(), totais.resultado(), totais.rentabilidadePercentual(),
                ultimaAtualizacao, composicao.size(), composicao);
    }

    private ComposicaoCarteiraResponse composicao(AtivoCarteira posicao) {
        PosicaoFinanceira valores = CalculadoraFinanceira.calcular(posicao.getQuantidade(), posicao.getPrecoMedio(),
                posicao.getAcao().getCotacaoAtual());
        return new ComposicaoCarteiraResponse(posicao.getId(), posicao.getAcao().getId(), posicao.getAcao().getMercado(),
                ClassificadorAlocacao.para(posicao.getAcao().getMercado()), posicao.getAcao().getTicker(),
                posicao.getAcao().getNomeEmpresa(), posicao.getQuantidade(), posicao.getAcao().getCotacaoAtual(),
                posicao.getAcao().getDataHoraCotacao(), valores.valorInvestido(), valores.valorAtual(), valores.resultado(),
                valores.rentabilidadePercentual());
    }
}
