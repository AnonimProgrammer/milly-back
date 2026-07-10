package com.milly.config.infrastructure.adapter.outbound.ai;

import com.milly.common.application.exception.AiServiceUnavailableException;
import com.milly.config.application.dto.AiResponse;
import com.milly.config.application.port.outbound.OrderPreparationAnalysisPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "milly.ai", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledOrderPreparationAnalysisAdapter implements OrderPreparationAnalysisPort {

    @Override
    public AiResponse estimatePreparationTime(String orderPreparationJson) {
        throw new AiServiceUnavailableException("AI service is not enabled.");
    }
}