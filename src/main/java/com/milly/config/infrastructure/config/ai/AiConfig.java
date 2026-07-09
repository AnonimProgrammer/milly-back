package com.milly.config.infrastructure.config.ai;

import com.milly.config.infrastructure.adapter.outbound.ai.langchain4j.MillyAssistant;
import com.milly.config.infrastructure.adapter.outbound.ai.langchain4j.OrderPreparationAnalyzer;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    @Bean
    @ConditionalOnProperty(prefix = "milly.ai", name = "enabled", havingValue = "true")
    ChatModel chatModel(AiProperties aiProperties) {
        AiProperties.OpenRouter openRouter = aiProperties.openRouter();
        String apiKey = openRouter.apiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException(
                    "milly.ai.enabled is true but OPENROUTER_API_KEY is not set. "
                            + "Add it to .env (Docker) or export it before starting the app.");
        }

        Map<String, String> customHeaders = new LinkedHashMap<>();
        if (StringUtils.hasText(openRouter.httpReferer())) {
            customHeaders.put("HTTP-Referer", openRouter.httpReferer());
        }
        if (StringUtils.hasText(openRouter.appTitle())) {
            customHeaders.put("X-Title", openRouter.appTitle());
        }

        var builder = OpenAiChatModel.builder()
                .baseUrl(openRouter.baseUrl())
                .apiKey(apiKey)
                .modelName(openRouter.modelName())
                .maxTokens(aiProperties.maxTokens())
                .logRequests(aiProperties.logRequests())
                .logResponses(aiProperties.logResponses());

        if (!customHeaders.isEmpty()) {
            builder.customHeaders(customHeaders);
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "milly.ai", name = "enabled", havingValue = "true")
    MillyAssistant millyAssistant(ChatModel chatModel) {
        return AiServices.builder(MillyAssistant.class)
                .chatModel(chatModel)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "milly.ai", name = "enabled", havingValue = "true")
    OrderPreparationAnalyzer orderPreparationAnalyzer(ChatModel chatModel) {
        return AiServices.builder(OrderPreparationAnalyzer.class)
                .chatModel(chatModel)
                .build();
    }
}
