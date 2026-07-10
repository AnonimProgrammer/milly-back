package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.IssueWsTicketResponse;
import com.milly.auth.application.port.outbound.WsTicketStore;
import com.milly.auth.domain.model.WsTicket;
import com.milly.auth.infrastructure.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IssueWsTicketUseCaseTest {

    private static final long WS_TICKET_TTL_SECONDS = 60;

    @Mock
    private WsTicketStore wsTicketStore;

    private IssueWsTicketUseCase issueWsTicketUseCase;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        AuthProperties authProperties = new AuthProperties(
                new AuthProperties.Jwt(
                        "test-jwt-secret-with-at-least-sixty-four-characters-for-hmac-signing",
                        900,
                        1_209_600),
                null,
                null,
                new AuthProperties.WsTicket(WS_TICKET_TTL_SECONDS));
        issueWsTicketUseCase = new IssueWsTicketUseCase(wsTicketStore, authProperties);
    }

    @Test
    void registersTicketWithConfiguredTtlAndReturnsResponse() {
        // Arrange
        Instant before = Instant.now();

        // Act
        IssueWsTicketResponse response = issueWsTicketUseCase.execute(userId);

        // Assert
        Instant after = Instant.now();
        ArgumentCaptor<WsTicket> ticketCaptor = ArgumentCaptor.forClass(WsTicket.class);
        verify(wsTicketStore).register(ticketCaptor.capture());

        WsTicket registeredTicket = ticketCaptor.getValue();
        assertThat(registeredTicket.userId()).isEqualTo(userId);
        assertThat(registeredTicket.ticketId()).isEqualTo(response.ticketId());
        assertThat(registeredTicket.issuedAt()).isBetween(before, after);
        assertThat(registeredTicket.expiresAt())
                .isEqualTo(registeredTicket.issuedAt().plusSeconds(WS_TICKET_TTL_SECONDS));
        assertThat(response.expiresAt())
                .isEqualTo(registeredTicket.expiresAt().atOffset(java.time.ZoneOffset.UTC));
        assertThat(Duration.between(registeredTicket.issuedAt(), registeredTicket.expiresAt()).toSeconds())
                .isEqualTo(WS_TICKET_TTL_SECONDS);
    }
}
