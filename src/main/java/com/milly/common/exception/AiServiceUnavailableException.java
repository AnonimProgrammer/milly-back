package com.milly.common.exception;

public class AiServiceUnavailableException extends RuntimeException {

    public static final String MESSAGE = "AI service is temporarily unavailable.";

    public AiServiceUnavailableException() {
        super(MESSAGE);
    }

    public AiServiceUnavailableException(String message) {
        super(message);
    }

    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
