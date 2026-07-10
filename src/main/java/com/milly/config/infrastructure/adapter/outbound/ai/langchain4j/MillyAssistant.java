package com.milly.config.infrastructure.adapter.outbound.ai.langchain4j;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface MillyAssistant {

    @SystemMessage(fromResource = "prompt/system-requirements.txt")
    String chat(@UserMessage String userMessage);
}