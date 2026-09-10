package com.example.carteirainvestimento.enums;

/** País do mercado em que o instrumento é negociado. */
public enum CountryCode {
    BR, US;
    public Mercado mercado() { return this == BR ? Mercado.BRASIL : Mercado.EUA; }
    public CountryCode outro() { return this == BR ? US : BR; }
}
