package com.milly.auth.application.dto;

public record RefreshSessionResponse(
        String accessToken,
        String refreshToken
) {
}
