package com.example.carteirainvestimento.dto.venda;
import com.example.carteirainvestimento.enums.Moeda;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record VendaRequest(@NotNull Long posicaoId, @NotNull @DecimalMin("0.000001") BigDecimal quantidade,
        @NotNull @DecimalMin("0.000001") BigDecimal precoVenda, @NotNull @DecimalMin("0") BigDecimal taxas,
        @NotNull LocalDate dataVenda, Moeda moeda) {}
