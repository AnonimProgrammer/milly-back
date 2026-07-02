package com.milly.auth.domain;

import com.milly.common.domain.Preconditions;
import com.milly.common.exception.InvalidCredentialsException;

import java.util.Map;

public final class Credentials {

    private Credentials() {}

    public static String requiredRaw(Map<String, Object> credentials, String... keys) {
        for (String key : keys) {
            String raw = optionalRaw(credentials, key);
            if (raw != null && !raw.isBlank()) {
                return raw;
            }
        }
        throw new InvalidCredentialsException("Missing required credential: " + keys[0] + ".");
    }

    public static String requiredNormalized(Map<String, Object> credentials, String... keys) {
        return requiredRaw(credentials, keys).trim().toLowerCase();
    }

    public static String optionalRaw(Map<String, Object> credentials, String key) {
        return Preconditions.optionalRaw(credentials, key);
    }
}
