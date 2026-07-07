package com.milly.config.infrastructure.config.idempotency;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "idempotency")
public record IdempotencyProperties(long ttlSeconds) {

    private static final long DEFAULT_TTL_SECONDS = 86_400;

    public IdempotencyProperties {
        if (ttlSeconds <= 0) {
            ttlSeconds = DEFAULT_TTL_SECONDS;
        }
    }
}
