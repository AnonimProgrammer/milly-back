package com.milly.config.infrastructure.adapter.outbound.ai.langchain4j;

import com.milly.common.exception.AiServiceUnavailableException;
import com.milly.config.application.dto.AiResponse;
import com.milly.config.application.port.outbound.AiChatPort;
import com.milly.config.application.port.outbound.OrderPreparationAnalysisPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "milly.ai", name = "enabled", havingValue = "true")
public class Langchain4jAiAdapter implements AiChatPort, OrderPreparationAnalysisPort {

    private final MillyAssistant millyAssistant;
    private final OrderPreparationAnalyzer orderPreparationAnalyzer;

    @Override
    @CircuitBreaker(name = "ai", fallbackMethod = "chatFallback")
    public AiResponse chat(String userMessage) {
        return new AiResponse(millyAssistant.chat(userMessage));
    }

    @Override
    @CircuitBreaker(name = "ai", fallbackMethod = "estimatePreparationTimeFallback")
    public AiResponse estimatePreparationTime(String orderPreparationJson) {
        return new AiResponse(orderPreparationAnalyzer.estimatePreparationTime(orderPreparationJson));
    }

    @SuppressWarnings("unused")
    private AiResponse chatFallback(String userMessage, Throwable cause) {
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
}