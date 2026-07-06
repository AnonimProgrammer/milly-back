package com.milly.config.websocket;

import com.milly.auth.application.port.outbound.WsTicketStore;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WsTicketHandshakeInterceptor implements HandshakeInterceptor {

    static final String TICKET_QUERY_PARAM = "ticket";

    private final WsTicketStore wsTicketStore;

    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes) {
        Optional<String> ticketValue = readTicket(request);
        if (ticketValue.isEmpty()) {
            return true;
        }

        Optional<UUID> ticketId = parseTicketId(ticketValue.get());
        if (ticketId.isEmpty()) {
            rejectHandshake(response);
            return false;
        }

        Optional<UUID> userId = wsTicketStore.claim(ticketId.get());
        if (userId.isEmpty()) {
            rejectHandshake(response);
            return false;
        }

        attributes.put(WebSocketSessionAttributes.USER_ID, userId.get());
        return true;
    }

    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            Exception exception) {
        // no-op
    }

    private Optional<String> readTicket(ServerHttpRequest request) {
        return Optional.ofNullable(
                UriComponentsBuilder.fromUri(request.getURI())
                        .build()
                        .getQueryParams()
                        .getFirst(TICKET_QUERY_PARAM));
    }

    private Optional<UUID> parseTicketId(String ticketValue) {
        if (ticketValue.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(ticketValue));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private void rejectHandshake(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
    }
}
