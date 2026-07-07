package com.milly.venue.infrastructure.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.milly.config.domain.constant.CacheNames;
import com.milly.config.domain.model.CacheSpec;
import com.milly.config.infrastructure.config.cache.CaffeineCacheFactory;
import com.milly.venue.domain.model.VenueInvitation;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.UUID;

@Configuration
public class VenueInvitationCacheConfig {

    private static final long VENUE_INVITATION_CACHE_MAX_SIZE = 10_000;

    @Bean
    Cache<UUID, VenueInvitation> venueInvitationCache(
            CaffeineCacheFactory cacheFactory, VenueProperties venueProperties) {
        Duration ttl = Duration.ofSeconds(venueProperties.invitation().ttlSeconds());
        return cacheFactory.buildCache(CacheSpec.of(ttl, VENUE_INVITATION_CACHE_MAX_SIZE));
    }

    @Bean
    @SuppressWarnings("unchecked")
    CaffeineCache venueInvitationsCache(Cache<UUID, VenueInvitation> venueInvitationCache) {
        return new CaffeineCache(
                CacheNames.VENUE_INVITATIONS,
                (Cache<Object, Object>) (Cache<?, ?>) venueInvitationCache);
    }
}
