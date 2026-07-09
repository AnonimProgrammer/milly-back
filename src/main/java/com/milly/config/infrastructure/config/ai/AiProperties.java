package com.milly.config.infrastructure.config.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "milly.ai")
public record AiProperties(
        boolean enabled,
        boolean logRequests,
        boolean logResponses,
        int maxTokens,
        OpenRouter openRouter
) {

    public AiProperties {
        if (maxTokens <= 0) {
            maxTokens = 1024;
        }
        if (openRouter == null) {
            openRouter = new OpenRouter(
                    "",
                    "google/gemini-2.0-flash-001",
                    "https://openrouter.ai/api/v1",
                    "http://localhost:3000",
                    "Milly");
        }
    }

    public record OpenRouter(
            String apiKey,
            String modelName,
            String baseUrl,
            String httpReferer,
            String appTitle) {

        public OpenRouter {
            if (modelName == null || modelName.isBlank()) {
                modelName = "google/gemini-2.0-flash-001";
            }
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "https://openrouter.ai/api/v1";
            }
            if (apiKey == null) {
                apiKey = "";
            }
            if (httpReferer == null) {
                httpReferer = "";
            }
            if (appTitle == null || appTitle.isBlank()) {
                appTitle = "Milly";
            }
        }
    }
}
