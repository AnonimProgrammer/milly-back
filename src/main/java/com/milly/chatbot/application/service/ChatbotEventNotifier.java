package com.milly.chatbot.application.service;

import com.milly.chatbot.application.dto.ChatMessageEvent;
import com.milly.chatbot.application.dto.ChatMessageType;
import com.milly.config.application.port.outbound.WsEventPublisher;
import com.milly.config.domain.constant.StompTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatbotEventNotifier {

    static final String WELCOME_TEXT = "Welcome to Milly!";

    private final WsEventPublisher wsEventPublisher;

    public void welcome(UUID tableId) {
        publish(tableId, ChatMessageType.WELCOME, WELCOME_TEXT);
    }

    public void assistantReply(UUID tableId, String text) {
        publish(tableId, ChatMessageType.ASSISTANT_REPLY, text);
    }

    public void error(UUID tableId, String text) {
        publish(tableId, ChatMessageType.ERROR, text);
    }

    private void publish(UUID tableId, ChatMessageType type, String text) {
        ChatMessageEvent event = new ChatMessageEvent(type, text);
        wsEventPublisher.publish(StompTopics.tableChatTopic(tableId), event);
    }
}
