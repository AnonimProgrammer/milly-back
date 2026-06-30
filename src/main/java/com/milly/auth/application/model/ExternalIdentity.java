package com.milly.auth.application.model;

import com.milly.auth.domain.valueobject.AuthProviderType;

public record ExternalIdentity(
        AuthProviderType provider,
        String providerUserId,
        String email
) {
}
