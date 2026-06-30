package com.milly.auth.application.exception;

public class RefreshSessionFailedException extends RuntimeException {

    public static final String MESSAGE = "Token is invalid.";

    public RefreshSessionFailedException() {
        super(MESSAGE);
    }
}
