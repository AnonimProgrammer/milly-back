package com.milly.common.application.exception;

public class InvalidInvitationException extends RuntimeException {

    public static final String MESSAGE = "Invitation is invalid or has expired.";

    public InvalidInvitationException() {
        super(MESSAGE);
    }
}