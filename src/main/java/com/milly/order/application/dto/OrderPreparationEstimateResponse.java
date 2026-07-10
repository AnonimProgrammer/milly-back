package com.milly.order.application.dto;

import java.util.UUID;

public record OrderPreparationEstimateResponse(
        UUID orderId,
        int minutes,
        String displayValue
) {
    public static OrderPreparationEstimateResponse of(UUID orderId, OrderPreparationAiResult result) {
        return new OrderPreparationEstimateResponse(orderId, result.minutes(), result.value());
    }
}