package com.milly.auth.application.dto;

import com.milly.auth.domain.valueobject.UserStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        UserStatus status,
        OffsetDateTime createdAt,
        List<String> roles) {
}
