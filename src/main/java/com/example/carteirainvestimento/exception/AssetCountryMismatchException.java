package com.example.carteirainvestimento.exception;

public class AssetCountryMismatchException extends ApplicationException {
    public AssetCountryMismatchException(String message) { super(ErrorCode.ASSET_COUNTRY_MISMATCH, message); }
}
