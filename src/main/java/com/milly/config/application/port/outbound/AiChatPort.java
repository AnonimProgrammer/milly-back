package com.milly.config.application.port.outbound;

import com.milly.config.application.dto.AiResponse;

public interface AiChatPort {

    AiResponse chat(String userMessage);
}
