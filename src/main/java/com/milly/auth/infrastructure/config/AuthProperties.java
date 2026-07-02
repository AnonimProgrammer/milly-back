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
        public Jwt {
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
