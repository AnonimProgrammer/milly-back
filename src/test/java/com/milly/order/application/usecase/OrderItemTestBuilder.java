package com.milly.order.application.usecase;

import com.milly.common.domain.valueobject.Money;
import com.milly.order.domain.entity.OrderItemEntity;

import java.util.UUID;

final class OrderItemTestBuilder {

    private UUID id = UUID.randomUUID();
    private UUID orderId = UUID.randomUUID();
    private UUID menuItemId = UUID.randomUUID();
    private int quantity = 1;
    private Money unitPrice = Money.of("10.00");

    private OrderItemTestBuilder() {
    }

    static OrderItemTestBuilder anOrderItem() {
        return new OrderItemTestBuilder();
    }

    OrderItemTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    OrderItemTestBuilder withOrderId(UUID orderId) {
        this.orderId = orderId;
        return this;
    }

    OrderItemTestBuilder withMenuItemId(UUID menuItemId) {
        this.menuItemId = menuItemId;
        return this;
    }

    OrderItemTestBuilder withQuantity(int quantity) {
        this.quantity = quantity;
        return this;
    }

    OrderItemTestBuilder withUnitPrice(Money unitPrice) {
        this.unitPrice = unitPrice;
        return this;
    }

    OrderItemEntity build() {
        OrderItemEntity item = OrderItemEntity.create(orderId, menuItemId, quantity, unitPrice);
        item.setId(id);
        return item;
    }
}
