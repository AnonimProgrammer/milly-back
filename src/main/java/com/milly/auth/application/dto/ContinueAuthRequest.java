package com.milly.auth.application.dto;

import com.milly.auth.domain.valueobject.AuthProviderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Map;

public record ContinueAuthRequest(
        @NotNull AuthProviderType provider,
        @NotNull Map<String, Object> credentials,
        @Valid UserProfileDto profile
) {

    public record UserProfileDto(
            String firstName,
            String lastName,
            String email,
            LocalDate birthDate
    ) {
    }
}
