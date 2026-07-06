package com.milly.config.websocket;

import com.milly.common.exception.AccessDeniedException;
import com.milly.venue.application.service.VenueAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TopicSubscriptionInterceptor implements ChannelInterceptor {

    private final VenueAuthorizationService venueAuthorizationService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        if (!isSubscriptionAllowed(accessor.getDestination(), accessor.getSessionAttributes())) {
            throw new AccessDeniedException();
        }

        return message;
    }

    private boolean isSubscriptionAllowed(@Nullable String destination, @Nullable Map<String, Object> sessionAttributes) {
        if (destination == null || sessionAttributes == null) {
            return false;
        }

        Optional<UUID> userId = readUserId(sessionAttributes);
        return userId.map(uuid -> isStaffSubscriptionAllowed(destination, uuid))
                .orElseGet(() -> isAnonymousSubscriptionAllowed(destination, sessionAttributes));

    }

    private boolean isStaffSubscriptionAllowed(String destination, UUID userId) {
        Optional<UUID> venueId = StompTopics.parseVenueStaffTopic(destination);
        if (venueId.isEmpty()) {
            return false;
        }

        try {
            venueAuthorizationService.requireMember(userId, venueId.get());
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean isAnonymousSubscriptionAllowed(String destination, Map<String, Object> sessionAttributes) {
        Optional<UUID> tableId = StompTopics.parseTableTopic(destination);
        if (tableId.isEmpty()) {
            return false;
        }

        Object boundTable = sessionAttributes.get(WebSocketSessionAttributes.BOUND_TABLE_ID);
        if (boundTable == null) {
            sessionAttributes.put(WebSocketSessionAttributes.BOUND_TABLE_ID, tableId.get());
            return true;
        }

        return tableId.get().equals(boundTable);
    }

    private Optional<UUID> readUserId(Map<String, Object> sessionAttributes) {
        Object value = sessionAttributes.get(WebSocketSessionAttributes.USER_ID);
        if (value instanceof UUID userId) {
            return Optional.of(userId);
        }
        return Optional.empty();
    }
}
