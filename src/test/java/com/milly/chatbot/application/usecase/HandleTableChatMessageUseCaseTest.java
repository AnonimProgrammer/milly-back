package com.milly.chatbot.application.usecase;

import com.milly.chatbot.application.dto.ChatHistoryMessage;
import com.milly.chatbot.application.dto.ChatInboundMessage;
import com.milly.chatbot.application.service.ChatbotEventNotifier;
import com.milly.common.application.exception.AiServiceUnavailableException;
import com.milly.common.application.exception.ResourceNotFoundException;
import com.milly.config.application.dto.AiChatMessage;
import com.milly.config.application.dto.AiResponse;
import com.milly.config.application.port.outbound.AiChatPort;
import com.milly.menu.application.dto.MenuItemResponse;
import com.milly.menu.application.usecase.ListPublicMenuItemsUseCase;
import com.milly.menu.domain.valueobject.MenuItemCategory;
import com.milly.menu.domain.valueobject.MenuItemStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandleTableChatMessageUseCaseTest {

    @Mock
    private ListPublicMenuItemsUseCase listPublicMenuItemsUseCase;

    @Mock
    private AiChatPort aiChatPort;

    @Mock
    private ChatbotEventNotifier chatbotEventNotifier;

    private HandleTableChatMessageUseCase useCase;

    private final UUID tableId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new HandleTableChatMessageUseCase(
                listPublicMenuItemsUseCase, aiChatPort, chatbotEventNotifier);
    }

    @Test
    void execute_publishesAssistantReply() {
        when(listPublicMenuItemsUseCase.execute(tableId)).thenReturn(List.of(menuItem("Pizza")));
        when(aiChatPort.chat(anyString(), anyList(), eq("What do you recommend?")))
                .thenReturn(new AiResponse("Try the Pizza."));

        useCase.execute(
                tableId,
                new ChatInboundMessage(
                        "What do you recommend?",
                        List.of(new ChatHistoryMessage("assistant", "Welcome to Milly!"))));

        ArgumentCaptor<String> menuCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AiChatMessage>> historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(aiChatPort).chat(menuCaptor.capture(), historyCaptor.capture(), eq("What do you recommend?"));
        assertThat(menuCaptor.getValue()).contains("Pizza");
        assertThat(historyCaptor.getValue())
                .containsExactly(new AiChatMessage("assistant", "Welcome to Milly!"));
        verify(chatbotEventNotifier).assistantReply(tableId, "Try the Pizza.");
    }

    @Test
    void execute_ignoresBlankText() {
        useCase.execute(tableId, new ChatInboundMessage("   ", List.of()));

        verify(listPublicMenuItemsUseCase, never()).execute(any());
        verify(aiChatPort, never()).chat(anyString(), anyList(), anyString());
        verify(chatbotEventNotifier, never()).assistantReply(any(), anyString());
    }

    @Test
    void execute_publishesErrorWhenAiUnavailable() {
        when(listPublicMenuItemsUseCase.execute(tableId)).thenReturn(List.of());
        when(aiChatPort.chat(anyString(), anyList(), eq("Hello")))
                .thenThrow(new AiServiceUnavailableException("AI service is not enabled."));

        useCase.execute(tableId, new ChatInboundMessage("Hello", List.of()));

        verify(chatbotEventNotifier).error(tableId, "AI service is not enabled.");
        verify(chatbotEventNotifier, never()).assistantReply(any(), anyString());
    }

    @Test
    void execute_publishesErrorWhenTableMissing() {
        when(listPublicMenuItemsUseCase.execute(tableId)).thenThrow(new ResourceNotFoundException());

        useCase.execute(tableId, new ChatInboundMessage("Hello", List.of()));

        verify(chatbotEventNotifier).error(tableId, "This table is not available.");
        verify(aiChatPort, never()).chat(anyString(), anyList(), anyString());
    }

    private static MenuItemResponse menuItem(String name) {
        UUID venueId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.parse("2026-01-01T12:00:00Z");
        return new MenuItemResponse(
                UUID.randomUUID(),
                venueId,
                name,
                "Tasty",
                new BigDecimal("12.50"),
                15,
                MenuItemCategory.MAINS,
                MenuItemStatus.ACTIVE,
                now,
                now);
    }
}
