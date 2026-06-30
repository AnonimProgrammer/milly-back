package com.milly.auth.application.model;

import com.milly.auth.domain.valueobject.RoleName;

import java.util.List;
import java.util.UUID;

public record AuthUser(
        UUID id,
        List<RoleName> roles
) {
}
