package com.milly.auth.application.port.outbound;

import com.milly.auth.domain.model.WsTicket;

import java.util.Optional;
import java.util.UUID;

public interface WsTicketStore {

    void register(WsTicket ticket);

    Optional<UUID> claim(UUID ticketId);
}
