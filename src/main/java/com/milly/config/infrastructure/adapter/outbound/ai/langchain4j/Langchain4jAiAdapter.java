package com.milly.config.infrastructure.adapter.outbound.ai.langchain4j;

import com.milly.common.application.exception.AiServiceUnavailableException;
import com.milly.config.application.dto.AiChatMessage;
import com.milly.config.application.dto.AiResponse;
import com.milly.config.application.port.outbound.AiChatPort;
import com.milly.config.application.port.outbound.OrderPreparationAnalysisPort;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "milly.ai", name = "enabled", havingValue = "true")
public class Langchain4jAiAdapter implements AiChatPort, OrderPreparationAnalysisPort {

    private static final String SYSTEM_PROMPT_TEMPLATE = loadSystemPromptTemplate();

    private final ChatModel chatModel;
    private final OrderPreparationAnalyzer orderPreparationAnalyzer;

    @Override
    @CircuitBreaker(name = "ai", fallbackMethod = "chatFallback")
    public AiResponse chat(String menuContext, List<AiChatMessage> history, String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(renderSystemPrompt(menuContext)));

        if (history != null) {
            for (AiChatMessage turn : history) {
                ChatMessage message = toChatMessage(turn);
                if (message != null) {
                    messages.add(message);
                }
            }
        }

        messages.add(UserMessage.from(userMessage));

        ChatResponse response = chatModel.chat(messages);
        String text = response.aiMessage() == null ? null : response.aiMessage().text();
        return new AiResponse(text);
    }

    @Override
    @CircuitBreaker(name = "ai", fallbackMethod = "estimatePreparationTimeFallback")
    public AiResponse estimatePreparationTime(String orderPreparationJson) {
        return new AiResponse(orderPreparationAnalyzer.estimatePreparationTime(orderPreparationJson));
    }

    @SuppressWarnings("unused")
    private AiResponse chatFallback(
            String menuContext, List<AiChatMessage> history, String userMessage, Throwable cause) {
        throw toUnavailable(cause);
    }

    @SuppressWarnings("unused")
    private AiResponse estimatePreparationTimeFallback(String orderPreparationJson, Throwable cause) {
        throw toUnavailable(cause);
    }

    private AiServiceUnavailableException toUnavailable(Throwable cause) {
        if (cause instanceof AiServiceUnavailableException unavailable) {
            return unavailable;
        }
        log.warn("AI provider call failed: {}", rootCauseMessage(cause), cause);
        return new AiServiceUnavailableException(AiServiceUnavailableException.MESSAGE, cause);
    }

    private static String rootCauseMessage(Throwable cause) {
        Throwable root = cause;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
    }

    private static String renderSystemPrompt(String menuContext) {
        String menu = StringUtils.hasText(menuContext) ? menuContext : "No menu items are currently available.";
        return SYSTEM_PROMPT_TEMPLATE.replace("{{menu}}", menu);
    }

    private static ChatMessage toChatMessage(AiChatMessage turn) {
        if (turn == null || !StringUtils.hasText(turn.content())) {
            return null;
        }
        String role = turn.role() == null ? "" : turn.role().trim().toLowerCase(Locale.ROOT);
        return switch (role) {
            case "user" -> UserMessage.from(turn.content());
            case "assistant" -> AiMessage.from(turn.content());
            default -> null;
        };
    }

    private static String loadSystemPromptTemplate() {
        ClassPathResource resource = new ClassPathResource("prompt/system-requirements.txt");
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load prompt/system-requirements.txt", ex);
        }
    }
}
