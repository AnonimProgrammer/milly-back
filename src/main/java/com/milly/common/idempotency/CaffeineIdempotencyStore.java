package com.milly.common.idempotency;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CaffeineIdempotencyStore implements IdempotencyStore {

    private final Cache<String, IdempotencyRecord> idempotencyCache;

    @Override
    public Optional<IdempotencyRecord> find(String key) {
        return Optional.ofNullable(idempotencyCache.getIfPresent(key));
    }

    @Override
    public void save(String key, IdempotencyRecord record) {
        idempotencyCache.put(key, record);
    }
}
