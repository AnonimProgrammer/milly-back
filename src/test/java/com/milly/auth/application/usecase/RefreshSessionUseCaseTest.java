package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.RefreshSessionResponse;
import com.milly.auth.application.exception.RefreshSessionFailedException;
import com.milly.auth.application.port.outbound.RefreshTokenStore;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.model.AuthUser;
import com.milly.auth.domain.model.IssuedRefreshToken;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.security.JwtTokenService;
import com.milly.common.exception.InvalidCredentialsException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.milly.auth.application.usecase.builder.UserTestBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshSessionUseCaseTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private UserJpaRepository userRepository;

    @Mock
    private LoadAuthUserUseCase loadAuthUserUseCase;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    private RefreshSessionUseCase refreshSessionUseCase;

    private final UUID userId = UUID.randomUUID();
    private final String refreshToken = "valid.refresh.token";
    private final String jti = "refresh-jti";
    private final String accessToken = "new.access.token";
    private final String newRefreshTokenValue = "new.refresh.token";
    private final String newRefreshJti = "new-refresh-jti";

    @BeforeEach
    void setUp() {
        refreshSessionUseCase = new RefreshSessionUseCase(
                jwtTokenService, userRepository, loadAuthUserUseCase, refreshTokenStore);
    }

    @Test
    void issuesNewTokensWhenRefreshTokenIsValid() {
        // Arrange
        Claims claims = mock(Claims.class);
        UserEntity user = aUser().withId(userId).build();
        AuthUser authUser = new AuthUser(userId, List.of(RoleName.USER));
        IssuedRefreshToken issuedRefreshToken = new IssuedRefreshToken(newRefreshTokenValue, newRefreshJti);
        when(jwtTokenService.parseToken(refreshToken, true)).thenReturn(claims);
        when(jwtTokenService.extractUserId(claims)).thenReturn(userId);
        when(jwtTokenService.extractJti(claims)).thenReturn(jti);
        when(refreshTokenStore.consume(jti, userId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(loadAuthUserUseCase.execute(user)).thenReturn(authUser);
        when(jwtTokenService.issueAccessToken(authUser)).thenReturn(accessToken);
        when(jwtTokenService.issueRefreshToken(authUser)).thenReturn(issuedRefreshToken);

        // Act
        RefreshSessionResponse response = refreshSessionUseCase.execute(refreshToken);

        // Assert
        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.refreshToken()).isEqualTo(newRefreshTokenValue);
        verify(refreshTokenStore).register(newRefreshJti, userId);
        verify(loadAuthUserUseCase).execute(user);
    }

    @Test
    void throwsWhenRefreshTokenIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> refreshSessionUseCase.execute(null))
                .isInstanceOf(RefreshSessionFailedException.class);

        verifyNoInteractions(jwtTokenService, userRepository, loadAuthUserUseCase, refreshTokenStore);
    }

    @Test
    void throwsWhenRefreshTokenIsBlank() {
        // Act & Assert
        assertThatThrownBy(() -> refreshSessionUseCase.execute("   "))
                .isInstanceOf(RefreshSessionFailedException.class);

        verifyNoInteractions(jwtTokenService, userRepository, loadAuthUserUseCase, refreshTokenStore);
    }

    @Test
    void throwsWhenTokenCannotBeParsed() {
        // Arrange
        when(jwtTokenService.parseToken(refreshToken, true))
                .thenThrow(new InvalidCredentialsException("Token is invalid."));

        // Act & Assert
        assertThatThrownBy(() -> refreshSessionUseCase.execute(refreshToken))
                .isInstanceOf(RefreshSessionFailedException.class);

        verifyNoInteractions(refreshTokenStore, userRepository, loadAuthUserUseCase);
    }

    @Test
    void throwsWhenRefreshTokenWasAlreadyConsumed() {
        // Arrange
        Claims claims = mock(Claims.class);
        when(jwtTokenService.parseToken(refreshToken, true)).thenReturn(claims);
        when(jwtTokenService.extractUserId(claims)).thenReturn(userId);
        when(jwtTokenService.extractJti(claims)).thenReturn(jti);
        when(refreshTokenStore.consume(jti, userId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> refreshSessionUseCase.execute(refreshToken))
                .isInstanceOf(RefreshSessionFailedException.class);

        verifyNoInteractions(userRepository, loadAuthUserUseCase);
    }

    @Test
    void throwsWhenUserNotFound() {
        // Arrange
        Claims claims = mock(Claims.class);
        when(jwtTokenService.parseToken(refreshToken, true)).thenReturn(claims);
        when(jwtTokenService.extractUserId(claims)).thenReturn(userId);
        when(jwtTokenService.extractJti(claims)).thenReturn(jti);
        when(refreshTokenStore.consume(jti, userId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> refreshSessionUseCase.execute(refreshToken))
                .isInstanceOf(RefreshSessionFailedException.class);

        verifyNoInteractions(loadAuthUserUseCase);
    }

    @Test
    void throwsWhenExtractingUserIdFails() {
        // Arrange
        Claims claims = mock(Claims.class);
        when(jwtTokenService.parseToken(refreshToken, true)).thenReturn(claims);
        when(jwtTokenService.extractUserId(claims)).thenThrow(new IllegalArgumentException("Invalid subject"));

        // Act & Assert
        assertThatThrownBy(() -> refreshSessionUseCase.execute(refreshToken))
                .isInstanceOf(RefreshSessionFailedException.class);

        verifyNoInteractions(refreshTokenStore, userRepository, loadAuthUserUseCase);
    }
}