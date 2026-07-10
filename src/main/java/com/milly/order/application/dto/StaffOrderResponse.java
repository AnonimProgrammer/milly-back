package com.milly.order.application.dto;

import com.milly.order.application.service.OrderTotalCalculator;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.entity.OrderItemEntity;
import com.milly.order.domain.valueobject.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StaffOrderResponse(
        UUID id,
        UUID tableId,
        OrderStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime closedAt,
        OffsetDateTime approvedAt,
        Integer estimatedPreparationMinutes,
        String estimatedPreparationDisplay,
        List<OrderItemResponse> items,
        BigDecimal paidAmount,
        BigDecimal remaining,
        BigDecimal totalTipAmount
) {

    public static StaffOrderResponse of(
            OrderEntity order,
            List<OrderItemEntity> items,
            BigDecimal paidAmount,
            BigDecimal totalTipAmount) {
        BigDecimal orderTotal = OrderTotalCalculator.totalOf(items);
        BigDecimal remaining = orderTotal.subtract(paidAmount).max(BigDecimal.ZERO);
        return new StaffOrderResponse(
                order.getId(),
                order.getTableId(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getClosedAt(),
                order.getApprovedAt(),
                order.getEstimatedPreparationMinutes(),
                order.getEstimatedPreparationDisplay(),
                items.stream().map(OrderItemResponse::of).toList(),
                paidAmount,
                remaining,
                totalTipAmount
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