package com.milly.auth.infrastructure.adapter.outbound.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.milly.auth.application.port.outbound.WsTicketStore;
import com.milly.auth.domain.model.WsTicket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CaffeineWsTicketStore implements WsTicketStore {

    private final Cache<UUID, WsTicket> wsTicketCache;

    @Override
    public void register(WsTicket ticket) {
        wsTicketCache.put(ticket.ticketId(), ticket);
    }

    @Override
    public Optional<UUID> claim(UUID ticketId) {
        WsTicket ticket = wsTicketCache.asMap().remove(ticketId);
        return ticket == null ? Optional.empty() : Optional.of(ticket.userId());
    }
}
