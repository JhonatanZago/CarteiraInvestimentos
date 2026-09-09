package com.example.carteirainvestimento.service;

import java.util.Map;

/** Registry curado por CNPJ; URLs apontam exclusivamente para sites oficiais. */
final class CorretoraLogoRegistry {
    private static final Map<String, String> LOGOS = Map.of(
            "02332886000104", "/assets/brokers/xp-investimentos.svg",
            "30306294000145", "/assets/brokers/btg-pactual.svg");

    private CorretoraLogoRegistry() { }

    static String logoFor(String cnpj) { return LOGOS.get(cnpj); }
}
