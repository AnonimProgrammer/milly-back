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
        ChatMessageEvent event = new ChatMessageEvent(ChatMessageType.WELCOME, WELCOME_TEXT);
        wsEventPublisher.publish(StompTopics.tableChatTopic(tableId), event);
    }
}