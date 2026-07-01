package com.milly.table.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTableLabelRequest(
        @NotBlank(message = "Label is required.")
        @Size(max = 100, message = "Label must be at most 100 characters.")
        String label
) {
}
