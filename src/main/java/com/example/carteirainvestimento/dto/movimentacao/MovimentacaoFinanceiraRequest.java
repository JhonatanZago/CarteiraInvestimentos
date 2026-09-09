package com.example.carteirainvestimento.dto.movimentacao;

import com.example.carteirainvestimento.enums.TipoMovimentacao;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Dados de uma operação individual; câmbio é informado quando conhecido. */
public record MovimentacaoFinanceiraRequest(
        TipoMovimentacao tipo,
        BigDecimal quantidade,
        BigDecimal precoUnitario,
        BigDecimal cambioParaBrl,
        java.time.OffsetDateTime cambioEm,
        String cambioFonte,
        BigDecimal custosBrl,
        LocalDate dataOperacao) {
}
