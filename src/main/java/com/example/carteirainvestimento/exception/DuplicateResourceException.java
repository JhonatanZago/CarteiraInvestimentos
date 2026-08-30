package com.example.carteirainvestimento.exception;

public class DuplicateResourceException extends ApplicationException {

    public DuplicateResourceException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE, message);
    }
}
