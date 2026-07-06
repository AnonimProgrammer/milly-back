package com.milly.config.websocket;

import com.milly.auth.application.port.outbound.WsTicketStore;
import com.milly.auth.domain.model.WsTicket;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class StompWebSocketIntegrationTest {

    private static final long CONNECT_TIMEOUT_SECONDS = 5;

    @LocalServerPort
    private int port;

    @Autowired
    private WsTicketStore wsTicketStore;

    @Autowired
    private VenueMembershipJpaRepository venueMembershipRepository;

    private UUID staffUserId;
    private UUID venueId;
    private UUID tableId;

    @BeforeEach
    void setUp() {
        staffUserId = UUID.randomUUID();
        venueId = UUID.randomUUID();
        tableId = UUID.randomUUID();

        venueMembershipRepository.save(VenueMembershipEntity.create(venueId, staffUserId, VenueRole.MANAGER));
    }

    @Test
    void customerConnectsWithoutTicket() throws Exception {
        StompSession session = connect(null).get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertThat(session.isConnected()).isTrue();
        session.disconnect();
    }

    @Test
    void staffConnectsWithValidTicket() throws Exception {
        UUID ticketId = UUID.randomUUID();
        registerTicket(ticketId, staffUserId);

        StompSession session = connect("?ticket=" + ticketId).get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertThat(session.isConnected()).isTrue();
        session.disconnect();
    }

    @Test
    void handshakeRejectsInvalidTicket() {
        ExecutionException exception = assertThrows(
                ExecutionException.class,
                () -> connect("?ticket=" + UUID.randomUUID()).get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        assertThat(exception.getCause()).isNotNull();
    }

    @Test
    void handshakeRejectsReusedTicket() throws Exception {
        UUID ticketId = UUID.randomUUID();
        registerTicket(ticketId, staffUserId);

        StompSession firstSession = connect("?ticket=" + ticketId).get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        firstSession.disconnect();

        assertThrows(
                ExecutionException.class,
                () -> connect("?ticket=" + ticketId).get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    void customerCanSubscribeToBoundTableTopic() throws Exception {
        StompSession session = connect(null).get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        StompSession.Subscription subscription = session.subscribe(
                "/topic/table/" + tableId,
                new NoOpStompFrameHandler());

        assertThat(subscription.getSubscriptionId()).isNotBlank();
        session.disconnect();
    }

    @Test
    void staffCanSubscribeToOwnVenueTopic() throws Exception {
        UUID ticketId = UUID.randomUUID();
        registerTicket(ticketId, staffUserId);

        StompSession session = connect("?ticket=" + ticketId).get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        StompSession.Subscription subscription = session.subscribe(
                "/topic/venue/" + venueId + "/staff",
                new NoOpStompFrameHandler());

        assertThat(subscription.getSubscriptionId()).isNotBlank();
        session.disconnect();
    }

    private void registerTicket(UUID ticketId, UUID userId) {
        Instant issuedAt = Instant.now();
        wsTicketStore.register(new WsTicket(ticketId, userId, issuedAt, issuedAt.plusSeconds(30)));
    }

    private CompletableFuture<StompSession> connect(String query) {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        String url = "ws://localhost:" + port + "/ws" + (query == null ? "" : query);
        return stompClient.connectAsync(url, new WebSocketHttpHeaders(), new NoOpStompSessionHandler());
    }
}
