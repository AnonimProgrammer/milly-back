package com.milly.chatbot.infrastructure.adapter.inbound.websocket;

import com.milly.chatbot.application.service.ChatbotEventNotifier;
import com.milly.config.domain.constant.StompTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatWelcomeOnSubscribeListener {

    private final ChatbotEventNotifier chatbotEventNotifier;

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Optional<UUID> tableId = StompTopics.parseTableChatTopic(accessor.getDestination());
        if (tableId.isEmpty()) {
            return;
        }

        chatbotEventNotifier.welcome(tableId.get());
    }
}