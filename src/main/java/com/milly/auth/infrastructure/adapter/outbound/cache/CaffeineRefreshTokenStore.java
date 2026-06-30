package com.milly.auth.infrastructure.adapter.outbound.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.milly.auth.application.port.outbound.RefreshTokenStore;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CaffeineRefreshTokenStore implements RefreshTokenStore {

    private final Cache<String, UUID> refreshTokenCache;

    public CaffeineRefreshTokenStore(Cache<String, UUID> refreshTokenCache) {
        this.refreshTokenCache = refreshTokenCache;
    }

    @Override
    public void register(String jti, UUID userId) {
        refreshTokenCache.put(jti, userId);
    }

    @Override
    public boolean consume(String jti, UUID userId) {
        return refreshTokenCache.asMap().remove(jti, userId);
    }

    @Override
    public void revoke(String jti) {
        refreshTokenCache.invalidate(jti);
    }
}
