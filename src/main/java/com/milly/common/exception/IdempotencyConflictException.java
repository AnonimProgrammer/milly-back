package com.milly.common.exception;

public class IdempotencyConflictException extends RuntimeException {

    public static final String MESSAGE = "Idempotency-Key was already used with a different request body.";

    public IdempotencyConflictException() {
        super(MESSAGE);
    }
}
