package com.milly.config.websocket;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CapturingStompFrameHandler implements StompFrameHandler {

    private final ObjectMapper objectMapper;
    private final CompletableFuture<JsonNode> future = new CompletableFuture<>();

    public CapturingStompFrameHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return byte[].class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        try {
            future.complete(objectMapper.readTree((byte[]) payload));
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }
    }

    public JsonNode awaitPayload(long timeout, TimeUnit unit) throws Exception {
        return future.get(timeout, unit);
    }
}
