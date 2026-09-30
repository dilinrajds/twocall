package com.twocall.chat.exception;

public class PairingCodeExpiredException extends RuntimeException {
    public PairingCodeExpiredException(String message) {
        super(message);
    }
}
