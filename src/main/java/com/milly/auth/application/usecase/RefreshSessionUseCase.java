package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.RefreshSessionResponse;
import com.milly.auth.application.exception.RefreshSessionFailedException;
import com.milly.auth.application.port.outbound.RefreshTokenStore;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.model.AuthUser;
import com.milly.auth.domain.model.IssuedRefreshToken;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.security.JwtTokenService;
import com.milly.common.exception.InvalidCredentialsException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshSessionUseCase {

    private final JwtTokenService jwtTokenService;
    private final UserJpaRepository userRepository;
    private final LoadAuthUserUseCase loadAuthUserUseCase;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional(readOnly = true)
    public RefreshSessionResponse execute(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RefreshSessionFailedException();
        }

        try {
            Claims claims = jwtTokenService.parseToken(refreshToken, true);
            UUID userId = jwtTokenService.extractUserId(claims);
            String jti = jwtTokenService.extractJti(claims);

            if (!refreshTokenStore.consume(jti, userId)) {
                throw new RefreshSessionFailedException();
            }

            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(RefreshSessionFailedException::new);

            AuthUser authUser = loadAuthUserUseCase.execute(user);
            String accessToken = jwtTokenService.issueAccessToken(authUser);
            IssuedRefreshToken newRefreshToken = jwtTokenService.issueRefreshToken(authUser);
            refreshTokenStore.register(newRefreshToken.jti(), userId);

            return new RefreshSessionResponse(accessToken, newRefreshToken.token());
        } catch (InvalidCredentialsException | IllegalArgumentException exception) {
            throw new RefreshSessionFailedException();
        }
    }
}
