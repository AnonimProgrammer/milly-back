package com.milly.order.application.usecase;

import com.milly.common.domain.valueobject.Money;
import com.milly.menu.domain.entity.MenuItemEntity;
import com.milly.menu.domain.valueobject.MenuItemStatus;

import java.util.UUID;

final class MenuItemTestBuilder {

    private UUID id = UUID.randomUUID();
    private UUID venueId = UUID.randomUUID();
    private String name = "Burger";
    private Money price = Money.of("10.00");
    private MenuItemStatus status = MenuItemStatus.ACTIVE;

    private MenuItemTestBuilder() {
    }

    static MenuItemTestBuilder aMenuItem() {
        return new MenuItemTestBuilder();
    }

    MenuItemTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    MenuItemTestBuilder withVenueId(UUID venueId) {
        this.venueId = venueId;
        return this;
    }

    MenuItemTestBuilder withPrice(Money price) {
        this.price = price;
        return this;
    }

    MenuItemTestBuilder withStatus(MenuItemStatus status) {
        this.status = status;
        return this;
    }

    MenuItemEntity build() {
        MenuItemEntity item = MenuItemEntity.create(venueId, name, null, price, status);
        item.setId(id);
        return item;
    }
}
