package com.milly.auth.application.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCurrentUserRequest(
        @Size(max = 100, message = "First name must be at most 100 characters.")
        String firstName,

        @Size(max = 100, message = "Last name must be at most 100 characters.")
        String lastName,

        @Pattern(
                regexp = "^$|^\\+?[1-9]\\d{1,14}$",
                message = "Phone number must be a valid international number.")
        @Size(max = 50, message = "Phone number must be at most 50 characters.")
        String phoneNumber
) {
}
