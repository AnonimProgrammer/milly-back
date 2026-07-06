package com.milly.config.infrastructure.config.cache;

import com.milly.config.domain.model.CacheSpec;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.caffeine.CaffeineCache;

import java.time.Duration;

@RequiredArgsConstructor
public class CaffeineCacheFactory {

    private final CacheProperties properties;

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
        return CacheSpec.of(expireAfterWrite, properties.defaultMaximumSize());
    }
}
