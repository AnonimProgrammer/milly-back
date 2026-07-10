package com.milly.order.application.usecase.builder;

import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.valueobject.OrderStatus;

import java.util.UUID;

public final class OrderTestBuilder {

    private UUID id = UUID.randomUUID();
    private UUID venueId = UUID.randomUUID();
    private UUID tableId = UUID.randomUUID();
    private OrderStatus status = OrderStatus.PENDING;

    private OrderTestBuilder() {
    }

    public static OrderTestBuilder anOrder() {
        return new OrderTestBuilder();
    }

    public OrderTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public OrderTestBuilder withVenueId(UUID venueId) {
        this.venueId = venueId;
        return this;
    }

    public OrderTestBuilder withTableId(UUID tableId) {
        this.tableId = tableId;
        return this;
    }

    public OrderTestBuilder withStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    public OrderEntity build() {
        OrderEntity order = OrderEntity.create(venueId, tableId, status);
        order.setId(id);
        return order;
    }
}