package com.milly.auth.infrastructure.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.milly.auth.domain.model.WsTicket;
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

    @Bean
    Cache<UUID, WsTicket> wsTicketCache(CaffeineCacheFactory cacheFactory, AuthProperties authProperties) {
        Duration ttl = Duration.ofSeconds(authProperties.wsTicket().ttlSeconds());
        return cacheFactory.buildCache(CacheSpec.of(ttl, WS_TICKET_CACHE_MAX_SIZE));
    }

    @Bean
    @SuppressWarnings("unchecked")
    CaffeineCache wsTicketsCache(Cache<UUID, WsTicket> wsTicketCache) {
        return new CaffeineCache(
                CacheNames.WS_TICKETS,
                (Cache<Object, Object>) (Cache<?, ?>) wsTicketCache);
    }
}
