package com.milly.config.infrastructure.config.idempotency;

import com.github.benmanes.caffeine.cache.Cache;
import com.milly.common.idempotency.IdempotencyRecord;
import com.milly.config.domain.model.CacheSpec;
import com.milly.config.infrastructure.config.cache.CaffeineCacheFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(IdempotencyProperties.class)
public class IdempotencyCacheConfig {

    private static final long IDEMPOTENCY_CACHE_MAX_SIZE = 50_000;

    @Bean
    Cache<String, IdempotencyRecord> idempotencyCache(
            CaffeineCacheFactory cacheFactory, IdempotencyProperties properties) {
        Duration ttl = Duration.ofSeconds(properties.ttlSeconds());
        return cacheFactory.buildCache(CacheSpec.of(ttl, IDEMPOTENCY_CACHE_MAX_SIZE));
    }
}
