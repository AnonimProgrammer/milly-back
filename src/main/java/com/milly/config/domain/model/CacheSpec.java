package com.milly.config.domain.model;

import java.time.Duration;

public record CacheSpec(
        Duration expireAfterWrite,
        long maximumSize
) {

    public CacheSpec {
        if (expireAfterWrite == null || expireAfterWrite.isZero() || expireAfterWrite.isNegative()) {
            throw new IllegalArgumentException("expireAfterWrite must be positive.");
        }
        if (maximumSize <= 0) {
            throw new IllegalArgumentException("maximumSize must be positive.");
        }
    }

    public static CacheSpec of(Duration expireAfterWrite, long maximumSize) {
        return new CacheSpec(expireAfterWrite, maximumSize);
    }
}
