package com.milly.config.infrastructure.adapter.inbound.websocket;

import com.milly.common.application.exception.AccessDeniedException;
import com.milly.config.domain.constant.WebSocketSessionAttributes;
import com.milly.venue.application.service.VenueAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicSubscriptionInterceptorTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    private TopicSubscriptionInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new TopicSubscriptionInterceptor(venueAuthorizationService);
    }

    @Test
    void allowsAnonymousTableSubscriptionAndBindsTable() {
        UUID tableId = UUID.randomUUID();
        Map<String, Object> sessionAttributes = new HashMap<>();

        assertDoesNotThrow(() -> interceptor.preSend(subscribeMessage("/topic/table/" + tableId, sessionAttributes), null));

        assertThatBoundTable(sessionAttributes, tableId);
    }

    @Test
    void rejectsAnonymousCrossTableSubscription() {
        UUID tableId = UUID.randomUUID();
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put(WebSocketSessionAttributes.BOUND_TABLE_ID, tableId);

        assertThrows(
                AccessDeniedException.class,
                () -> interceptor.preSend(
                        subscribeMessage("/topic/table/" + UUID.randomUUID(), sessionAttributes),
                        null));
    }

    @Test
    void rejectsAnonymousStaffTopicSubscription() {
        UUID venueId = UUID.randomUUID();

        assertThrows(
                AccessDeniedException.class,
                () -> interceptor.preSend(
                        subscribeMessage("/topic/venue/" + venueId + "/staff", new HashMap<>()),
                        null));
    }

    @Test
    void allowsStaffVenueSubscriptionForMember() {
        UUID userId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        Map<String, Object> sessionAttributes = Map.of(WebSocketSessionAttributes.USER_ID, userId);

        assertDoesNotThrow(() -> interceptor.preSend(
                subscribeMessage("/topic/venue/" + venueId + "/staff", sessionAttributes),
                null));

        verify(venueAuthorizationService).requireMember(userId, venueId);
    }

    @Test
    void rejectsStaffCrossVenueSubscription() {
        UUID userId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        Map<String, Object> sessionAttributes = Map.of(WebSocketSessionAttributes.USER_ID, userId);

        when(venueAuthorizationService.requireMember(userId, venueId)).thenThrow(new AccessDeniedException());

        assertThrows(
                AccessDeniedException.class,
                () -> interceptor.preSend(
                        subscribeMessage("/topic/venue/" + venueId + "/staff", sessionAttributes),
                        null));
    }

    private Message<byte[]> subscribeMessage(String destination, Map<String, Object> sessionAttributes) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSessionAttributes(sessionAttributes);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private void assertThatBoundTable(Map<String, Object> sessionAttributes, UUID tableId) {
        Object boundTable = sessionAttributes.get(WebSocketSessionAttributes.BOUND_TABLE_ID);
        if (!tableId.equals(boundTable)) {
            throw new AssertionError("Expected bound table " + tableId + " but was " + boundTable);
        }
    }
}
