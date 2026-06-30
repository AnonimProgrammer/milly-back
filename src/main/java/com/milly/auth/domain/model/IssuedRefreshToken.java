package com.milly.auth.domain.model;

public record IssuedRefreshToken(
        String token,
        String jti
) {
}
