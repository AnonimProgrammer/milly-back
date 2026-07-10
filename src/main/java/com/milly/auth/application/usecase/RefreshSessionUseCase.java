package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.RefreshSessionResponse;
import com.milly.auth.application.exception.RefreshSessionFailedException;
import com.milly.auth.application.port.outbound.RefreshTokenStore;
import com.milly.auth.application.port.outbound.SessionTokenPort;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.model.AuthUser;
import com.milly.auth.domain.model.IssuedRefreshToken;
import com.milly.auth.domain.model.ParsedRefreshToken;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserJpaRepository;
import com.milly.common.application.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshSessionUseCase {

    private final SessionTokenPort sessionTokenPort;
    private final UserJpaRepository userRepository;
    private final LoadAuthUserUseCase loadAuthUserUseCase;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional(readOnly = true)
    public RefreshSessionResponse execute(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RefreshSessionFailedException();
        }

        try {
            ParsedRefreshToken parsed = sessionTokenPort.parseRefreshToken(refreshToken);
            UUID userId = parsed.userId();
            String jti = parsed.jti();

            if (!refreshTokenStore.consume(jti, userId)) {
                throw new RefreshSessionFailedException();
            }

            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(RefreshSessionFailedException::new);

            AuthUser authUser = loadAuthUserUseCase.execute(user);
            String accessToken = sessionTokenPort.issueAccessToken(authUser);
            IssuedRefreshToken newRefreshToken = sessionTokenPort.issueRefreshToken(authUser);
            refreshTokenStore.register(newRefreshToken.jti(), userId);

            return new RefreshSessionResponse(accessToken, newRefreshToken.token());
        } catch (InvalidCredentialsException | IllegalArgumentException exception) {
            throw new RefreshSessionFailedException();
        }
    }
}
