package com.milly.auth.application.dto;

import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.domain.valueobject.UserStatus;

import java.util.List;

public record UpdateAdminUserRequest(
        UserStatus status,
        List<RoleName> roles) {
}
