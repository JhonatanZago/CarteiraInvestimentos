package com.example.carteirainvestimento.service;

import java.util.Map;

/** Registry curado por ticker exato, usado apenas quando a fonte não fornece logo. */
final class AcaoLogoRegistry {
    private static final Map<String, String> LOGOS = Map.of(
            "PETR4", "/assets/assets/petr4.svg",
            "VALE3", "/assets/assets/vale3.svg",
            "ITUB4", "/assets/assets/itub4.svg",
            "AAPL", "/assets/assets/aapl.svg",
            "AERI3", "/assets/assets/aeri3.svg",
            "BRST3", "/assets/assets/brst3.svg");

    private AcaoLogoRegistry() { }

    static String logoFor(String ticker) { return LOGOS.get(ticker.trim().toUpperCase()); }
}
