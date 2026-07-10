package com.milly.order.application.dto;

import com.milly.order.application.service.OrderTotalCalculator;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.entity.OrderItemEntity;
import com.milly.order.domain.valueobject.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID tableId,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal paidAmount,
        BigDecimal remaining,
        OffsetDateTime createdAt,
        OffsetDateTime approvedAt,
        Integer estimatedPreparationMinutes,
        String estimatedPreparationDisplay
) {

    public static OrderResponse of(OrderEntity order, List<OrderItemEntity> items, BigDecimal paidAmount) {
        BigDecimal orderTotal = OrderTotalCalculator.totalOf(items);
        BigDecimal remaining = orderTotal.subtract(paidAmount).max(BigDecimal.ZERO);
        return new OrderResponse(
                order.getId(),
                order.getTableId(),
                order.getStatus(),
                items.stream().map(OrderItemResponse::of).toList(),
                paidAmount,
                remaining,
                order.getCreatedAt(),
                order.getApprovedAt(),
                order.getEstimatedPreparationMinutes(),
                order.getEstimatedPreparationDisplay()
        );
    }

    public record OrderItemResponse(
            UUID id,
            UUID menuItemId,
            Integer quantity,
            BigDecimal unitPrice
    ) {
        public static OrderItemResponse of(OrderItemEntity item) {
            return new OrderItemResponse(
                    item.getId(),
                    item.getMenuItemId(),
                    item.getQuantity(),
                    item.getUnitPrice().amount()
            );
        }
    }
}