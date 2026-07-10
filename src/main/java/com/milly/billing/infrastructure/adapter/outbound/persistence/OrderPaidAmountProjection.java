package com.milly.billing.infrastructure.adapter.outbound.persistence;

import java.math.BigDecimal;
import java.util.UUID;

public interface OrderPaidAmountProjection {
    UUID getOrderId();
    BigDecimal getPaidAmount();
}