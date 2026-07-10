package com.milly.auth.application.dto;

import com.milly.auth.domain.valueobject.AuthProviderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record ContinueAuthRequest(
        @NotNull(message = "Provider is required.") AuthProviderType provider,
        @NotNull(message = "Credentials are required.") Map<String, Object> credentials,
        @Valid UserProfileDto profile
) {

    public record UserProfileDto(
            @NotBlank(message = "First name is required.")
            @Size(max = 100, message = "First name must be at most 100 characters.")
            String firstName,
            @NotBlank(message = "Last name is required.")
            @Size(max = 100, message = "Last name must be at most 100 characters.")
            String lastName,
            @NotBlank(message = "Email is required.")
            @Email(message = "Email must be valid.")
            @Size(max = 255, message = "Email must be at most 255 characters.")
            String email
    ) {}
}