package com.milly.auth.application.port.outbound;

import java.util.Optional;
import java.util.UUID;

public interface WsTicketStore {

    void register(UUID ticketId, UUID userId);

    Optional<UUID> claim(UUID ticketId);
}
