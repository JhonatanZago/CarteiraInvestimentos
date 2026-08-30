package com.example.carteirainvestimento.dto.dashboard;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

public record DashboardCarteiraResponse(
        Long carteiraId,
        BigDecimal valorInvestido,
        BigDecimal valorAtual,
        BigDecimal resultado,
        BigDecimal rentabilidadePercentual,
        OffsetDateTime ultimaAtualizacao,
        @Schema(description = "Numero de posicoes que compoem a carteira", example = "3") int quantidadeAtivos,
        @Schema(description = "Detalhamento calculado de cada posicao da carteira") List<ComposicaoCarteiraResponse> composicao) {
}
