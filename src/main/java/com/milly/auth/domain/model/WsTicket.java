package com.milly.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record WsTicket(
        UUID ticketId,
        UUID userId,
        Instant issuedAt,
        Instant expiresAt
) {
}
