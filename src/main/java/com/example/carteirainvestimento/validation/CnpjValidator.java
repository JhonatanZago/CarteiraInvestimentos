package com.example.carteirainvestimento.validation;

public final class CnpjValidator {

    private static final int[] FIRST_DIGIT_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] SECOND_DIGIT_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjValidator() {
    }

    public static boolean isValid(String cnpj) {
        if (cnpj == null || !cnpj.matches("\\d{14}") || cnpj.chars().distinct().count() == 1) {
            return false;
        }
        return calculateDigit(cnpj.substring(0, 12), FIRST_DIGIT_WEIGHTS) == Character.getNumericValue(cnpj.charAt(12))
                && calculateDigit(cnpj.substring(0, 13), SECOND_DIGIT_WEIGHTS) == Character.getNumericValue(cnpj.charAt(13));
    }

    private static int calculateDigit(String value, int[] weights) {
        int sum = 0;
        for (int index = 0; index < weights.length; index++) {
            sum += Character.getNumericValue(value.charAt(index)) * weights[index];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
