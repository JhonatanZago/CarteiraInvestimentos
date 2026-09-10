package com.example.carteirainvestimento.dto.venda;
import com.example.carteirainvestimento.enums.Moeda;
import java.math.BigDecimal;
import java.time.LocalDate;
public record VendaResponse(Long id, Long carteiraId, Long posicaoId, String ticker, String nomeEmpresa,
        String logoUrl, BigDecimal quantidade, BigDecimal quantidadeDisponivelAntes, BigDecimal quantidadeRestante,
        BigDecimal precoMedio, BigDecimal precoVenda, BigDecimal taxas, BigDecimal valorBruto, BigDecimal custoPosicao,
        BigDecimal resultadoRealizado, BigDecimal rentabilidadePercentual, Moeda moeda, LocalDate dataVenda) {}
