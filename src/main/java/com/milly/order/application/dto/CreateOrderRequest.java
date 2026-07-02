package com.milly.order.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotEmpty(message = "At least one item is required.")
        @Valid
        List<ItemDto> items
) {
    public record ItemDto(
            @NotNull(message = "menuItemId is required.")
            UUID menuItemId,

            @NotNull(message = "quantity is required.")
            @Min(value = 1, message = "quantity must be at least 1.")
            Integer quantity
    ) {}
}
