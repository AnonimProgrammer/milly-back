package com.milly.config.client;

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
}
