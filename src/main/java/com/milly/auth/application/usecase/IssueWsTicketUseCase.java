package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.IssueWsTicketResponse;
import com.milly.auth.application.port.outbound.WsTicketStore;
import com.milly.auth.domain.model.WsTicket;
import com.milly.auth.infrastructure.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IssueWsTicketUseCase {

    private final WsTicketStore wsTicketStore;
    private final AuthProperties authProperties;

    public IssueWsTicketResponse execute(UUID userId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(authProperties.wsTicket().ttlSeconds());
        UUID ticketId = UUID.randomUUID();
        WsTicket ticket = new WsTicket(ticketId, userId, issuedAt, expiresAt);
        wsTicketStore.register(ticket);
        return new IssueWsTicketResponse(ticketId, expiresAt.atOffset(ZoneOffset.UTC));
    }
}
