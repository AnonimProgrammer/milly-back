package com.milly.auth.infrastructure.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.milly.config.cache.CacheNames;
import com.milly.config.cache.CacheSpec;
import com.milly.config.cache.CaffeineCacheFactory;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.UUID;

@Configuration
public class WsTicketCacheConfig {

    private static final long WS_TICKET_CACHE_MAX_SIZE = 10_000;
    private static final Duration WS_TICKET_TTL = Duration.ofSeconds(30);

    @Bean
    Cache<UUID, UUID> wsTicketCache(CaffeineCacheFactory cacheFactory) {
        return cacheFactory.buildCache(CacheSpec.of(WS_TICKET_TTL, WS_TICKET_CACHE_MAX_SIZE));
    }

    @Bean
    @SuppressWarnings("unchecked")
    CaffeineCache wsTicketsCache(Cache<UUID, UUID> wsTicketCache) {
        return new CaffeineCache(
                CacheNames.WS_TICKETS,
                (Cache<Object, Object>) (Cache<?, ?>) wsTicketCache);
    }
}
