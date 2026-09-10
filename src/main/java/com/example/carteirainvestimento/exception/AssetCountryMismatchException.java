package com.example.carteirainvestimento.exception;

public class AssetCountryMismatchException extends AssetMarketMismatchException {
    public AssetCountryMismatchException(String message) { super(ErrorCode.ASSET_COUNTRY_MISMATCH, message); }
}
