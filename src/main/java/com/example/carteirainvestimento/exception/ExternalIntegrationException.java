package com.example.carteirainvestimento.exception;

public class ExternalIntegrationException extends ApplicationException {

    public ExternalIntegrationException(String message) {
        super(ErrorCode.EXTERNAL_INTEGRATION_ERROR, message);
    }
}
