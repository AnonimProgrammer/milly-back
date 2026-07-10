package com.milly.billing.application.polluter;

import java.util.UUID;

public record UnpayableOrder(
        UUID venueId,
        UUID tableId,
        UUID orderId
) {
}