package com.milly.chatbot.application.dto;

import java.util.List;

public record ChatInboundMessage(String text, List<ChatHistoryMessage> history) {

    public ChatInboundMessage {
        history = history == null ? List.of() : List.copyOf(history);
    }
}
