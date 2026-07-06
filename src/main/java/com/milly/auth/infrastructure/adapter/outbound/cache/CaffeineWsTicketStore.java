package com.milly.auth.infrastructure.adapter.outbound.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.milly.auth.application.port.outbound.WsTicketStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CaffeineWsTicketStore implements WsTicketStore {

    private final Cache<UUID, UUID> wsTicketCache;

    @Override
    public void register(UUID ticketId, UUID userId) {
        wsTicketCache.put(ticketId, userId);
    }

    @Override
    public Optional<UUID> claim(UUID ticketId) {
        UUID userId = wsTicketCache.asMap().remove(ticketId);
        return Optional.ofNullable(userId);
    }
}
