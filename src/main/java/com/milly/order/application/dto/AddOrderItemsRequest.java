package com.milly.order.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddOrderItemsRequest(
        @NotEmpty(message = "At least one item is required.")
        @Valid
        List<CreateOrderRequest.ItemDto> items
) {
}
