package com.example.carteirainvestimento.mapper;

import com.example.carteirainvestimento.domain.AtivoCarteira;
import com.example.carteirainvestimento.dto.carteira.AtivoCarteiraResponse;
import com.example.carteirainvestimento.service.CalculadoraFinanceira;
import com.example.carteirainvestimento.service.PosicaoFinanceira;
import com.example.carteirainvestimento.service.ClassificadorAlocacao;

public final class AtivoCarteiraMapper {

    private AtivoCarteiraMapper() {
    }

    public static AtivoCarteiraResponse toResponse(AtivoCarteira ativo) {
        PosicaoFinanceira valores = CalculadoraFinanceira.calcular(ativo.getQuantidade(), ativo.getPrecoMedio(),
                ativo.getAcao().getCotacaoAtual());
        return new AtivoCarteiraResponse(ativo.getId(), ativo.getCarteira().getId(), ativo.getAcao().getId(),
                ativo.getCorretora().getId(), ativo.getQuantidade(), ativo.getPrecoMedio(), ativo.getDataPrimeiraCompra(),
                ativo.getAcao().getMercado(), ClassificadorAlocacao.para(ativo.getAcao().getMercado()),
                ativo.getAcao().getTicker(), ativo.getAcao().getNomeEmpresa(), ativo.getAcao().getCotacaoAtual(),
                ativo.getAcao().getDataHoraCotacao(), valores.valorInvestido(), valores.valorAtual(), valores.resultado(),
                valores.rentabilidadePercentual());
    }
}
