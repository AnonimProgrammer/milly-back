package com.milly.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        Jwt jwt,
        Google google,
        Apple apple
) {

    public AuthProperties {
        if (google == null) {
            google = new Google("");
        }
        if (apple == null) {
            apple = new Apple("");
        }
    }

    public record Jwt(
            String secret,
            long accessTtlSeconds,
            long refreshTtlSeconds
    ) {
        static final String DEV_FALLBACK_SECRET =
                "dev-jwt-secret-with-at-least-sixty-four-characters-for-hmac-signing";

        public Jwt {
            if (secret == null || secret.isBlank()) {
                secret = DEV_FALLBACK_SECRET;
            }
            if (accessTtlSeconds <= 0) {
                accessTtlSeconds = 900;
            }
            if (refreshTtlSeconds <= 0) {
                refreshTtlSeconds = 1_209_600;
            }
        }
    }

    public record Google(String clientId) {
    }

    public record Apple(String clientId) {
    }
}
