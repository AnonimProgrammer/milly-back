package com.milly.common.application.exception;

public class InactiveMembershipException extends RuntimeException {

    public static final String MESSAGE = "Your access to this venue has been blocked.";

    public InactiveMembershipException() {
        super(MESSAGE);
    }
}
