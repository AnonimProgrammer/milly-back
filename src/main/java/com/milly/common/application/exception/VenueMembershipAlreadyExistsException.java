package com.milly.common.application.exception;

public class VenueMembershipAlreadyExistsException extends RuntimeException {

    public static final String MESSAGE = "You are already a member of this venue.";

    public VenueMembershipAlreadyExistsException() {
        super(MESSAGE);
    }
}