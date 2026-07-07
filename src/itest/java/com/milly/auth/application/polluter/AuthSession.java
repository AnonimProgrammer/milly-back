package com.milly.auth.application.polluter;

import java.util.UUID;

public record AuthSession(
        UUID userId,
        String email,
        String password,
        String accessToken
) {
}
