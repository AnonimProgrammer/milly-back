package com.milly.config.infrastructure.adapter.outbound.ai;

import com.milly.common.application.exception.AiServiceUnavailableException;
import com.milly.config.application.dto.AiChatMessage;
import com.milly.config.application.dto.AiResponse;
import com.milly.config.application.port.outbound.AiChatPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "milly.ai", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledAiChatAdapter implements AiChatPort {

    @Override
    public AiResponse chat(String menuContext, List<AiChatMessage> history, String userMessage) {
        throw new AiServiceUnavailableException("AI service is not enabled.");
    }
}
