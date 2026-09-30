package com.twocall.chat.exception;

public class PairLimitExceededException extends RuntimeException {
    public PairLimitExceededException(String message) {
        super(message);
    }
}
