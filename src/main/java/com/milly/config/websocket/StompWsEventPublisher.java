package com.milly.config.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class StompWsEventPublisher implements WsEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);
    }

    @Override
    public void publishAfterCommit(String destination, Object payload) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish(destination, payload);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish(destination, payload);
            }
        });
    }
}
