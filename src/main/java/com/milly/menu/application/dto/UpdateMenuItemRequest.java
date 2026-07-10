package com.milly.menu.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateMenuItemRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "Name must not be blank.")
        @Size(max = 255, message = "Name must be at most 255 characters.")
        String name,

        @Size(max = 2000, message = "Description must be at most 2000 characters.")
        String description,

        @DecimalMin(value = "0.01", message = "Price must be at least 0.01.")
        @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer and 2 fraction digits.")
        BigDecimal price,

        @Min(value = 1, message = "Approximate preparation time must be at least 1 minute.")
        @Max(value = 480, message = "Approximate preparation time must be at most 480 minutes.")
        Integer approximatePreparationMinutes
) {
}