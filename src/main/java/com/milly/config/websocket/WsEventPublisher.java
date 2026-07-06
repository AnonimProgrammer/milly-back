package com.milly.config.websocket;

public interface WsEventPublisher {

    void publish(String destination, Object payload);

    void publishAfterCommit(String destination, Object payload);
}
