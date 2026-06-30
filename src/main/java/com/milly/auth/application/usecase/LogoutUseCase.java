package com.milly.auth.application.usecase;

import com.milly.auth.application.port.outbound.RefreshTokenStore;
import com.milly.auth.infrastructure.adapter.outbound.security.JwtTokenService;
import com.milly.common.exception.InvalidCredentialsException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutUseCase {

    private final JwtTokenService jwtTokenService;
    private final RefreshTokenStore refreshTokenStore;

    public void execute(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        try {
            Claims claims = jwtTokenService.parseToken(refreshToken, true);
            refreshTokenStore.revoke(jwtTokenService.extractJti(claims));
        } catch (InvalidCredentialsException | IllegalArgumentException ignored) {
            // Idempotent: invalid or expired refresh tokens still complete logout.
        }
    }
}
