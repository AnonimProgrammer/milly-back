package com.milly.chatbot.application.usecase;

import com.milly.chatbot.application.dto.ChatHistoryMessage;
import com.milly.chatbot.application.dto.ChatInboundMessage;
import com.milly.chatbot.application.service.ChatbotEventNotifier;
import com.milly.chatbot.application.service.MenuContextFormatter;
import com.milly.common.application.exception.AiServiceUnavailableException;
import com.milly.common.application.exception.ResourceNotFoundException;
import com.milly.config.application.dto.AiChatMessage;
import com.milly.config.application.dto.AiResponse;
import com.milly.config.application.port.outbound.AiChatPort;
import com.milly.menu.application.usecase.ListPublicMenuItemsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandleTableChatMessageUseCase {

    private static final int MAX_HISTORY_MESSAGES = 10;

    private final ListPublicMenuItemsUseCase listPublicMenuItemsUseCase;
    private final AiChatPort aiChatPort;
    private final ChatbotEventNotifier chatbotEventNotifier;

    @Transactional(readOnly = true)
    public void execute(UUID tableId, ChatInboundMessage message) {
        String text = message == null || message.text() == null ? "" : message.text().trim();
        if (!StringUtils.hasText(text)) {
            return;
        }

        try {
            String menuContext = MenuContextFormatter.format(listPublicMenuItemsUseCase.execute(tableId));
            List<AiChatMessage> history = toAiHistory(message.history());
            AiResponse response = aiChatPort.chat(menuContext, history, text);
            String reply = response.rawContent() == null ? "" : response.rawContent().trim();
            if (!StringUtils.hasText(reply)) {
                chatbotEventNotifier.error(tableId, "I could not generate a reply. Please try again.");
                return;
            }
            chatbotEventNotifier.assistantReply(tableId, reply);
        } catch (AiServiceUnavailableException ex) {
            chatbotEventNotifier.error(tableId, ex.getMessage());
        } catch (ResourceNotFoundException ex) {
            chatbotEventNotifier.error(tableId, "This table is not available.");
        } catch (RuntimeException ex) {
            log.warn("Failed to handle chat message for table {}: {}", tableId, ex.getMessage(), ex);
            chatbotEventNotifier.error(tableId, "Something went wrong. Please try again.");
        }
    }

    private static List<AiChatMessage> toAiHistory(List<ChatHistoryMessage> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }

        int fromIndex = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
        return history.subList(fromIndex, history.size()).stream()
                .filter(item -> item != null && StringUtils.hasText(item.content()))
                .map(item -> {
                    String role = normalizeRole(item.role());
                    return role == null ? null : new AiChatMessage(role, item.content().trim());
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private static String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return null;
        }
        return switch (role.trim().toLowerCase(Locale.ROOT)) {
            case "user" -> "user";
            case "assistant" -> "assistant";
            default -> null;
        };
    }
}
