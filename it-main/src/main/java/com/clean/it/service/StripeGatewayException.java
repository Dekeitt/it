package com.clean.it.service;

public class StripeGatewayException extends RuntimeException {
    public StripeGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
