package com.milly.order.application.usecase.builder;

import com.milly.common.domain.valueobject.Money;
import com.milly.order.domain.entity.OrderItemEntity;

import java.util.UUID;

public final class OrderItemTestBuilder {

    private UUID id = UUID.randomUUID();
    private UUID orderId = UUID.randomUUID();
    private UUID menuItemId = UUID.randomUUID();
    private int quantity = 1;
    private Money unitPrice = Money.of("10.00");

    private OrderItemTestBuilder() {
    }

    public static OrderItemTestBuilder anOrderItem() {
        return new OrderItemTestBuilder();
    }

    public OrderItemTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public OrderItemTestBuilder withOrderId(UUID orderId) {
        this.orderId = orderId;
        return this;
    }

    public OrderItemTestBuilder withMenuItemId(UUID menuItemId) {
        this.menuItemId = menuItemId;
        return this;
    }

    public OrderItemTestBuilder withQuantity(int quantity) {
        this.quantity = quantity;
        return this;
    }

    public OrderItemTestBuilder withUnitPrice(Money unitPrice) {
        this.unitPrice = unitPrice;
        return this;
    }

    public OrderItemEntity build() {
        OrderItemEntity item = OrderItemEntity.create(orderId, menuItemId, quantity, unitPrice);
        item.setId(id);
        return item;
    }
}
