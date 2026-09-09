package com.example.carteirainvestimento.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.example.carteirainvestimento.dto.insight.ClassificacaoAlocacao;
import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;

public record ComposicaoCarteiraResponse(
        Long posicaoId,
        Long acaoId,
        Mercado mercado,
        ClassificacaoAlocacao classificacaoAlocacao,
        @Schema(description = "Codigo de negociacao da acao", example = "PETR4") String ticker,
        @Schema(description = "Nome da empresa emissora", example = "Petrobras") String nomeEmpresa,
        @Schema(description = "URL HTTPS do logotipo retornada pela fonte de cotacao") String logoUrl,
        BigDecimal quantidade,
        @Schema(description = "Preco medio ponderado das unidades abertas", example = "28.50") BigDecimal precoMedio,
        @Schema(description = "Ultima cotacao armazenada da acao", example = "31.25") BigDecimal cotacaoAtual,
        @Schema(description = "Instante da ultima cotacao armazenada", example = "2026-08-26T15:30:00Z") OffsetDateTime dataHoraCotacao,
        @Schema(description = "Quantidade multiplicada pelo preco medio", example = "2850.00") BigDecimal valorInvestido,
        @Schema(description = "Quantidade multiplicada pela ultima cotacao", example = "3125.00") BigDecimal valorAtual,
        @Schema(description = "Valor atual menos valor investido", example = "275.00") BigDecimal resultado,
        @Schema(description = "Resultado percentual calculado no backend", example = "9.6491") BigDecimal rentabilidadePercentual,
        Moeda moeda,
        boolean conversaoEstimada,
        BigDecimal valorInvestidoConvertidoBase,
        BigDecimal valorAtualConvertidoBase,
        BigDecimal cambioParaBase) {
    public ComposicaoCarteiraResponse(Long posicaoId, Long acaoId, Mercado mercado, ClassificacaoAlocacao classificacaoAlocacao,
            String ticker, String nomeEmpresa, String logoUrl, BigDecimal quantidade, BigDecimal cotacaoAtual,
            OffsetDateTime dataHoraCotacao, BigDecimal valorInvestido, BigDecimal valorAtual, BigDecimal resultado,
            BigDecimal rentabilidadePercentual) {
        this(posicaoId, acaoId, mercado, classificacaoAlocacao, ticker, nomeEmpresa, logoUrl, quantidade, null, cotacaoAtual,
                dataHoraCotacao, valorInvestido, valorAtual, resultado, rentabilidadePercentual, null, false, null, null, null);
    }
}
