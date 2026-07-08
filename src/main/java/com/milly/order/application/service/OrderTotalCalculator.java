package com.milly.order.application.service;

import com.milly.order.domain.entity.OrderItemEntity;

import java.math.BigDecimal;
import java.util.List;

/**
 * Computes an order's total from its line items. Shared by the order module's own response DTOs
 * and by billing (which is allowed to read order/order-item data, per the billing flow doc).
 */
public final class OrderTotalCalculator {

    private OrderTotalCalculator() {
    }

    public static BigDecimal totalOf(List<OrderItemEntity> items) {
        return items.stream()
                .map(OrderItemEntity::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
