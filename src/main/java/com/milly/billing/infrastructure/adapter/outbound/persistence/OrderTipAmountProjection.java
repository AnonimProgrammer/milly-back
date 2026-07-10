package com.milly.billing.infrastructure.adapter.outbound.persistence;

import java.math.BigDecimal;
import java.util.UUID;

public interface OrderTipAmountProjection {
    UUID getOrderId();
    BigDecimal getTipAmount();
}
