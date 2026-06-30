package com.milly.config.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cache.caffeine")
public record CacheProperties(long defaultMaximumSize) {

    public CacheProperties {
        if (defaultMaximumSize <= 0) {
            defaultMaximumSize = 10_000;
        }
    }
}
