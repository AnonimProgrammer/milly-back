package com.milly.auth.application.usecase;

import com.milly.auth.application.port.outbound.RefreshTokenStore;
import com.milly.auth.application.port.outbound.SessionTokenPort;
import com.milly.auth.domain.model.ParsedRefreshToken;
import com.milly.common.application.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutUseCase {

    private final SessionTokenPort sessionTokenPort;
    private final RefreshTokenStore refreshTokenStore;

    public void execute(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        try {
            ParsedRefreshToken parsed = sessionTokenPort.parseRefreshToken(refreshToken);
            refreshTokenStore.revoke(parsed.jti());
        } catch (InvalidCredentialsException | IllegalArgumentException ignored) {
            // Idempotent: invalid or expired refresh tokens still complete logout.
        }
    }
}
