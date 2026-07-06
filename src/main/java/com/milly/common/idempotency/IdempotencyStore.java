package com.milly.common.idempotency;

import java.util.Optional;

public interface IdempotencyStore {

    Optional<IdempotencyRecord> find(String key);

    void save(String key, IdempotencyRecord record);
}
