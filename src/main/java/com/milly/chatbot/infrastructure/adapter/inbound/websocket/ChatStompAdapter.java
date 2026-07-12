package com.milly.chatbot.infrastructure.adapter.inbound.websocket;

import com.milly.chatbot.application.dto.ChatInboundMessage;
import com.milly.chatbot.application.usecase.HandleTableChatMessageUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatStompAdapter {

    private final HandleTableChatMessageUseCase handleTableChatMessageUseCase;

    @MessageMapping("/table/{tableId}/chat")
    public void handleMessage(@DestinationVariable UUID tableId, @Payload ChatInboundMessage message) {
        handleTableChatMessageUseCase.execute(tableId, message);
    }
}
