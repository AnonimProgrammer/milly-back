package com.milly.auth.infrastructure.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.milly.config.domain.constant.CacheNames;
import com.milly.config.domain.model.CacheSpec;
import com.milly.config.infrastructure.config.cache.CaffeineCacheFactory;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.UUID;

@Configuration
public class AuthCacheConfig {

    private static final long REFRESH_TOKEN_CACHE_MAX_SIZE = 100_000;

    @Bean
    Cache<String, UUID> refreshTokenCache(CaffeineCacheFactory cacheFactory, AuthProperties authProperties) {
        Duration ttl = Duration.ofSeconds(authProperties.jwt().refreshTtlSeconds());
        return cacheFactory.buildCache(CacheSpec.of(ttl, REFRESH_TOKEN_CACHE_MAX_SIZE));
    }

    @Bean
    @SuppressWarnings("unchecked")
    CaffeineCache refreshTokensCache(Cache<String, UUID> refreshTokenCache) {
        return new CaffeineCache(
                CacheNames.REFRESH_TOKENS,
                (Cache<Object, Object>) (Cache<?, ?>) refreshTokenCache);
    }
}
