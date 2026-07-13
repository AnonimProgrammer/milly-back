package com.milly.config.infrastructure.config.cache;

import com.milly.config.domain.model.CacheSpec;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class CaffeineCacheFactory {

    public <K, V> Cache<K, V> buildCache(CacheSpec spec) {
        return Caffeine.newBuilder()
                .maximumSize(spec.maximumSize())
                .expireAfterWrite(spec.expireAfterWrite())
                .build();
    }
}
