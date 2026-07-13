package com.milly.auth.application.dto;

import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.domain.valueobject.UserStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateAdminUserRequest(
        UserStatus status,
        @Size(min = 1, message = "Roles must not be empty.")
        List<RoleName> roles) {

    @AssertTrue(message = "At least one of status or roles must be provided.")
    public boolean isAtLeastOneFieldPresent() {
        return status != null || roles != null;
    }
}
