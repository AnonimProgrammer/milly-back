package com.milly.common.application.exception;

public class UserAccountInactiveException extends RuntimeException {

    public static final String MESSAGE = "Your account has been suspended.";

    public UserAccountInactiveException() {
        super(MESSAGE);
    }
}
