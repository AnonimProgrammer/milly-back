package com.milly.order.application.port.outbound;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read-only lookup of how much has been paid toward an order. Owned by the order module (it is
 * the consumer) and implemented by an adapter in billing, so order never depends on billing.
 */
public interface PaymentSummaryPort {

    BigDecimal paidAmountFor(UUID orderId);

    Map<UUID, BigDecimal> paidAmountsFor(List<UUID> orderIds);

    BigDecimal tipAmountFor(UUID orderId);

    Map<UUID, BigDecimal> tipAmountsFor(List<UUID> orderIds);
}