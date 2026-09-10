package com.example.carteirainvestimento.exception;

public class AssetMarketMismatchException extends ApplicationException {
    public AssetMarketMismatchException(String message) {
        super(ErrorCode.ASSET_MARKET_MISMATCH, message);
    }
    protected AssetMarketMismatchException(ErrorCode code, String message) { super(code, message); }
}
