package com.milly.order.application.service;

import com.milly.config.application.dto.AiResponse;
import com.milly.order.application.dto.OrderPreparationAiResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OrderPreparationAiMapper {

    private final ObjectMapper objectMapper;

    public OrderPreparationAiMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OrderPreparationAiResult toResult(AiResponse response) {
        try {
            String content = stripMarkdown(response.rawContent());
            return objectMapper.readValue(content, OrderPreparationAiResult.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to parse AI preparation time response.");
        }
    }

    private static String stripMarkdown(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstLineBreak = trimmed.indexOf('\n');
            int closingFence = trimmed.lastIndexOf("```");
            if (firstLineBreak >= 0 && closingFence > firstLineBreak) {
                return trimmed.substring(firstLineBreak + 1, closingFence).trim();
            }
        }
        return trimmed;
    }
}
