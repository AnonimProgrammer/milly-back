package com.milly.menu.application.dto;

import com.milly.menu.domain.entity.MenuItemEntity;
import com.milly.menu.domain.valueobject.MenuItemStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MenuItemResponse(
        UUID id,
        UUID venueId,
        String name,
        String description,
        BigDecimal price,
        MenuItemStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static MenuItemResponse of(MenuItemEntity menuItem) {
        return new MenuItemResponse(
                menuItem.getId(),
                menuItem.getVenueId(),
                menuItem.getName(),
                menuItem.getDescription(),
                menuItem.getPrice().amount(),
                menuItem.getStatus(),
                menuItem.getCreatedAt(),
                menuItem.getUpdatedAt());
    }
}
