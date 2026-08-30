package com.example.carteirainvestimento.dto.historico;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.example.carteirainvestimento.enums.FonteCotacao;

public record HistoricoCotacaoResponse(Long id, Long acaoId, BigDecimal valor, OffsetDateTime dataHoraCotacao,
                                       OffsetDateTime dataHoraRegistro, FonteCotacao fonte) {
}
