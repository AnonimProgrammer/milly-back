package com.milly.common.application.port.outbound;

import com.milly.common.application.idempotency.IdempotencyRecord;

import java.util.Optional;

public interface IdempotencyStore {

    Optional<IdempotencyRecord> find(String key);

    void save(String key, IdempotencyRecord record);
}
