package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.dto.insight.IndicadorMercadoResponse;
import java.util.List;

/** Normalized boundary for market providers; provider payloads never leave this layer. */
public interface MarketIndicatorAdapter { List<IndicadorMercadoResponse> buscarIndicadores(); }
