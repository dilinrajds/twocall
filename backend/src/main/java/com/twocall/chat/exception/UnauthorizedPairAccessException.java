package com.twocall.chat.exception;

public class UnauthorizedPairAccessException extends RuntimeException {
    public UnauthorizedPairAccessException(String message) {
        super(message);
    }
}
