package com.milly.chatbot.application.dto;

public record ChatMessageEvent(ChatMessageType type, String text) {
}
