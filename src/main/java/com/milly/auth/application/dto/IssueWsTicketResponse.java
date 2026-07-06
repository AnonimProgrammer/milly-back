package com.milly.auth.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IssueWsTicketResponse(
        UUID ticketId,
        OffsetDateTime expiresAt
) {
}
