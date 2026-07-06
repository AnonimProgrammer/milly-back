package com.milly.config.application.port.outbound;

public interface WsEventPublisher {

    void publish(String destination, Object payload);

    void publishAfterCommit(String destination, Object payload);
}
