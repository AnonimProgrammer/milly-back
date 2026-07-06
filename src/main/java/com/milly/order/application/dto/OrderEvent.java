package com.milly.order.application.dto;

import java.util.UUID;

public record OrderEvent(
        OrderEventType type,
        UUID orderId,
        UUID venueId,
        UUID tableId) {
}
