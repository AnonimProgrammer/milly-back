package com.milly.order.application.dto;

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
        List<OrderItemResponse> items
) {

    public static StaffOrderResponse of(OrderEntity order, List<OrderItemEntity> items) {
        return new StaffOrderResponse(
                order.getId(),
                order.getTableId(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getClosedAt(),
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
