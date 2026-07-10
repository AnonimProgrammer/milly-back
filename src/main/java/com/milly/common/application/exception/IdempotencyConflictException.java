package com.milly.common.application.exception;

public class IdempotencyConflictException extends RuntimeException {

    public static final String MESSAGE = "Idempotency-Key was already used with a different request body.";

    public IdempotencyConflictException() {
        super(MESSAGE);
    }
}
