package com.example.carteirainvestimento.dto.carteira;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import com.example.carteirainvestimento.dto.insight.ClassificacaoAlocacao;
import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;

public record AtivoCarteiraResponse(
        Long id,
        Long carteiraId,
        Long acaoId,
        Long corretoraId,
        BigDecimal quantidade,
        BigDecimal precoMedio,
        LocalDate dataPrimeiraCompra,
        Mercado mercado,
        ClassificacaoAlocacao classificacaoAlocacao,
        @Schema(description = "Codigo de negociacao da acao", example = "PETR4") String ticker,
        @Schema(description = "Nome da empresa emissora", example = "Petrobras") String nomeEmpresa,
        @Schema(description = "URL HTTPS do logotipo retornada pela fonte de cotacao") String logoUrl,
        @Schema(description = "Ultima cotacao armazenada da acao", example = "31.25") BigDecimal cotacaoAtual,
        @Schema(description = "Instante da ultima cotacao armazenada", example = "2026-08-26T15:30:00Z") OffsetDateTime dataHoraCotacao,
        @Schema(description = "Quantidade multiplicada pelo preco medio", example = "2850.00") BigDecimal valorInvestido,
        @Schema(description = "Quantidade multiplicada pela ultima cotacao", example = "3125.00") BigDecimal valorAtual,
        @Schema(description = "Valor atual menos valor investido", example = "275.00") BigDecimal resultado,
        @Schema(description = "Resultado percentual calculado no backend", example = "9.6491") BigDecimal rentabilidadePercentual,
        Moeda moeda,
        boolean conversaoEstimada,
        BigDecimal valorInvestidoBrl,
        BigDecimal valorAtualBrl,
        BigDecimal cambioAtualParaBrl,
        OffsetDateTime cambioAtualEm,
        String cambioAtualFonte) {
    public AtivoCarteiraResponse(Long id, Long carteiraId, Long acaoId, Long corretoraId, BigDecimal quantidade, BigDecimal precoMedio,
            LocalDate dataPrimeiraCompra, Mercado mercado, ClassificacaoAlocacao classificacaoAlocacao, String ticker,
            String nomeEmpresa, String logoUrl, BigDecimal cotacaoAtual, OffsetDateTime dataHoraCotacao,
            BigDecimal valorInvestido, BigDecimal valorAtual, BigDecimal resultado, BigDecimal rentabilidadePercentual) {
        this(id, carteiraId, acaoId, corretoraId, quantidade, precoMedio, dataPrimeiraCompra, mercado, classificacaoAlocacao,
                ticker, nomeEmpresa, logoUrl, cotacaoAtual, dataHoraCotacao, valorInvestido, valorAtual, resultado,
                rentabilidadePercentual, Moeda.BRL, false, null, null, null, null, null);
    }
}
