package com.ledgerservice.service;

public class TransferException extends RuntimeException {

    private final String code;

    public TransferException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
