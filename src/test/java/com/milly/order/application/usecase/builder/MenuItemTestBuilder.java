package com.milly.order.application.usecase.builder;

import com.milly.common.domain.valueobject.Money;
import com.milly.menu.domain.entity.MenuItemEntity;
import com.milly.menu.domain.valueobject.MenuItemStatus;

import java.util.UUID;

public final class MenuItemTestBuilder {

    private UUID id = UUID.randomUUID();
    private UUID venueId = UUID.randomUUID();
    private String name = "Burger";
    private Money price = Money.of("10.00");
    private MenuItemStatus status = MenuItemStatus.ACTIVE;

    private MenuItemTestBuilder() {
    }

    public static MenuItemTestBuilder aMenuItem() {
        return new MenuItemTestBuilder();
    }

    public MenuItemTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public MenuItemTestBuilder withVenueId(UUID venueId) {
        this.venueId = venueId;
        return this;
    }

    public MenuItemTestBuilder withPrice(Money price) {
        this.price = price;
        return this;
    }

    public MenuItemTestBuilder withStatus(MenuItemStatus status) {
        this.status = status;
        return this;
    }

    public MenuItemEntity build() {
        MenuItemEntity item = MenuItemEntity.create(venueId, name, null, price, 15, status);
        item.setId(id);
        return item;
    }
}