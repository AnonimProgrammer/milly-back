package com.milly.config.infrastructure.adapter.outbound.ai.langchain4j;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface OrderPreparationAnalyzer {

    @SystemMessage(fromResource = "prompt/order-preparation-analyzer.txt")
    String estimatePreparationTime(@UserMessage String orderPreparationJson);
}