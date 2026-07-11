package com.milly.config.infrastructure.config.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "client")
public record ClientProperties(
        String url
) {
    public ClientProperties {
        if (url == null || url.isBlank()) {
            url = "http://localhost:3000";
        }
    }

    /** Normalized browser origin (scheme + host + port) for CORS and WebSocket. */
    public String origin() {
        String normalized = url.stripTrailing();
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
