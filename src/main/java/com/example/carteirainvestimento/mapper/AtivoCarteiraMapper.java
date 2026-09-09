package com.example.carteirainvestimento.mapper;

import com.example.carteirainvestimento.domain.AtivoCarteira;
import com.example.carteirainvestimento.dto.carteira.AtivoCarteiraResponse;
import com.example.carteirainvestimento.service.CalculadoraFinanceira;
import com.example.carteirainvestimento.service.PosicaoFinanceira;
import com.example.carteirainvestimento.service.ClassificadorAlocacao;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.example.carteirainvestimento.enums.Moeda;

public final class AtivoCarteiraMapper {

    private AtivoCarteiraMapper() {
    }

    public static AtivoCarteiraResponse toResponse(AtivoCarteira ativo) {
        return toResponse(ativo, null, null, null);
    }

    public static AtivoCarteiraResponse toResponse(AtivoCarteira ativo, BigDecimal cambioAtualParaBrl,
            OffsetDateTime cambioAtualEm, String cambioAtualFonte) {
        PosicaoFinanceira valores = CalculadoraFinanceira.calcular(ativo.getQuantidade(), ativo.getPrecoMedio(),
                ativo.getAcao().getCotacaoAtual());
        Moeda moeda = ativo.getAcao().getMoeda();
        BigDecimal investidoBrl = converter(valores.valorInvestido(), moeda, cambioAtualParaBrl);
        BigDecimal atualBrl = converter(valores.valorAtual(), moeda, cambioAtualParaBrl);
        return new AtivoCarteiraResponse(ativo.getId(), ativo.getCarteira().getId(), ativo.getAcao().getId(),
                ativo.getCorretora().getId(), ativo.getQuantidade(), ativo.getPrecoMedio(), ativo.getDataPrimeiraCompra(),
                ativo.getAcao().getMercado(), ClassificadorAlocacao.para(ativo.getAcao().getMercado()),
                ativo.getAcao().getTicker(), ativo.getAcao().getNomeEmpresa(), ativo.getAcao().getLogoUrl(), ativo.getAcao().getCotacaoAtual(),
                ativo.getAcao().getDataHoraCotacao(), valores.valorInvestido(), valores.valorAtual(), valores.resultado(),
                valores.rentabilidadePercentual(), moeda, ativo.isConversaoEstimada(), investidoBrl, atualBrl,
                cambioAtualParaBrl == null ? null : (moeda == Moeda.BRL ? BigDecimal.ONE : cambioAtualParaBrl),
                cambioAtualEm, cambioAtualFonte);
    }

    private static BigDecimal converter(BigDecimal valor, Moeda moeda, BigDecimal cambio) {
        if (valor == null) return null;
        if (cambio == null || cambio.signum() <= 0) return null;
        if (moeda == Moeda.BRL) return valor;
        return valor.multiply(cambio);
    }
}
