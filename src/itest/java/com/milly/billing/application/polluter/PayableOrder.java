package com.milly.billing.application.polluter;

import java.math.BigDecimal;
import java.util.UUID;

public record PayableOrder(
        UUID venueId,
        UUID tableId,
        UUID orderId,
        BigDecimal orderTotal
) {
}
