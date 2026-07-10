package com.milly.order.application.dto;

import java.util.List;
import java.util.UUID;

public record OrderPreparationAnalysisPayload(
        UUID orderId,
        int lineItemCount,
        int totalQuantity,
        List<Item> items
) {
    public record Item(
            String name,
            int quantity,
            int approximatePreparationMinutes
    ) {
    }
}