package com.milly.common.domain;

import com.milly.common.exception.InvalidCredentialsException;

import java.util.Map;

public final class Preconditions {

    private Preconditions() {
    }

    public static <T> T required(T value, String message) {
        if (value == null || (value instanceof String string && string.isBlank())) {
            throw new InvalidCredentialsException(message);
        }
        return value;
    }

    public static String optionalRaw(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim().toLowerCase();
        }
        if (second != null && !second.isBlank()) {
            return second.trim().toLowerCase();
        }
        return null;
    }
}
