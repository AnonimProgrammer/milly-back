package com.milly.venue.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVenueRequest(
        @NotBlank(message = "Name is required.")
        @Size(max = 255, message = "Name must be at most 255 characters.")
        String name,

        @NotBlank(message = "Location is required.")
        String location
) {
}
