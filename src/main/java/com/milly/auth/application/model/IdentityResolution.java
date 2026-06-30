package com.milly.auth.application.model;

import com.milly.auth.domain.entity.UserEntity;

public record IdentityResolution(UserEntity user, boolean newUser) {
}
