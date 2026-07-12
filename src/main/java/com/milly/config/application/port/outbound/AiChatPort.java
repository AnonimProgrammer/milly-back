package com.milly.config.application.port.outbound;

import com.milly.config.application.dto.AiChatMessage;
import com.milly.config.application.dto.AiResponse;

import java.util.List;

public interface AiChatPort {

    AiResponse chat(String menuContext, List<AiChatMessage> history, String userMessage);
}
