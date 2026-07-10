package com.milly.common.application.exception;

public class InvalidStateTransitionException extends RuntimeException {

    public static final String MESSAGE = "Invalid state transition.";

    public InvalidStateTransitionException() {
        super(MESSAGE);
    }
}
