package com.milly.order.application.usecase;

import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.valueobject.OrderStatus;

import java.util.UUID;

final class OrderTestBuilder {

    private UUID id = UUID.randomUUID();
    private UUID venueId = UUID.randomUUID();
    private UUID tableId = UUID.randomUUID();
    private OrderStatus status = OrderStatus.PENDING;

    private OrderTestBuilder() {
    }

    static OrderTestBuilder anOrder() {
        return new OrderTestBuilder();
    }

    OrderTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    OrderTestBuilder withVenueId(UUID venueId) {
        this.venueId = venueId;
        return this;
    }

    OrderTestBuilder withTableId(UUID tableId) {
        this.tableId = tableId;
        return this;
    }

    OrderTestBuilder withStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    OrderEntity build() {
        OrderEntity order = OrderEntity.create(venueId, tableId, status);
        order.setId(id);
        return order;
    }
}
