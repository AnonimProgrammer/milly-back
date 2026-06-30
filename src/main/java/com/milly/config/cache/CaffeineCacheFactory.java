package com.milly.config.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.caffeine.CaffeineCache;

import java.time.Duration;

public class CaffeineCacheFactory {

    private final long defaultMaximumSize;

    public CaffeineCacheFactory(CacheProperties properties) {
        this.defaultMaximumSize = properties.defaultMaximumSize();
    }

    public CaffeineCache buildSpringCache(String name, CacheSpec spec) {
        return new CaffeineCache(name, buildNativeCache(spec));
    }

    public <K, V> Cache<K, V> buildCache(CacheSpec spec) {
        return buildNativeCache(spec);
    }

    private <K, V> Cache<K, V> buildNativeCache(CacheSpec spec) {
        return Caffeine.newBuilder()
                .maximumSize(spec.maximumSize())
                .expireAfterWrite(spec.expireAfterWrite())
                .build();
    }

    public CacheSpec specWithDefaultMaxSize(Duration expireAfterWrite) {
        return CacheSpec.of(expireAfterWrite, defaultMaximumSize);
    }
}
