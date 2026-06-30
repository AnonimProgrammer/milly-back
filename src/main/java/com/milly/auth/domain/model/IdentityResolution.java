package com.milly.auth.domain.model;

import com.milly.auth.domain.entity.UserEntity;

public record IdentityResolution(UserEntity user, boolean newUser) {
}
