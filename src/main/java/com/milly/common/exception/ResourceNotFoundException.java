package com.milly.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    public static final String MESSAGE = "Resource not found.";

    public ResourceNotFoundException() {
        super(MESSAGE);
    }
}
