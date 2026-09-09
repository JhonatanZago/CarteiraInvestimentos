package com.example.carteirainvestimento.exception;

public class AssetInUseException extends ApplicationException {
    public AssetInUseException(String message) {
        super(ErrorCode.ASSET_IN_USE, message);
    }
}
