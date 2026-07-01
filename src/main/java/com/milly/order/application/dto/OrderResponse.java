package com.milly.order.application.dto;

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
        OffsetDateTime createdAt,
        List<OrderItemResponse> items
) {

    public static OrderResponse of(OrderEntity order, List<OrderItemEntity> items) {
        return new OrderResponse(
                order.getId(),
                order.getTableId(),
                order.getStatus(),
                order.getCreatedAt(),
                items.stream().map(OrderItemResponse::of).toList()
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
