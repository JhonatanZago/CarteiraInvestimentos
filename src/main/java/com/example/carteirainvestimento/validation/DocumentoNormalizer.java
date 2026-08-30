package com.example.carteirainvestimento.validation;

import com.example.carteirainvestimento.exception.BusinessRuleException;

public final class DocumentoNormalizer {

    private DocumentoNormalizer() {
    }

    public static String cnpj(String value) {
        return normalizar(value, 14, "CNPJ");
    }

    public static String cep(String value) {
        return normalizar(value, 8, "CEP");
    }

    private static String normalizar(String value, int tamanhoEsperado, String documento) {
        String normalizado = value == null ? "" : value.replaceAll("\\D", "");
        if (normalizado.length() != tamanhoEsperado) {
            throw new BusinessRuleException(documento + " deve conter " + tamanhoEsperado + " digitos");
        }
        return normalizado;
    }
}
