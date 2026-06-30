package com.milly.auth.application.dto;

public record ContinueAuthResponse(
        String accessToken,
        String refreshToken,
        boolean newUser
) {
}
