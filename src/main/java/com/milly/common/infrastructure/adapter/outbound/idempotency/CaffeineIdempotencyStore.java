package com.milly.common.infrastructure.adapter.outbound.idempotency;

import com.github.benmanes.caffeine.cache.Cache;
import com.milly.common.application.idempotency.IdempotencyRecord;
import com.milly.common.application.port.outbound.IdempotencyStore;
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
