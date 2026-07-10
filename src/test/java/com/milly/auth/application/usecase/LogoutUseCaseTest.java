package com.milly.auth.application.usecase;

import com.milly.auth.application.port.outbound.RefreshTokenStore;
import com.milly.auth.infrastructure.adapter.outbound.security.JwtTokenService;
import com.milly.common.exception.InvalidCredentialsException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    private LogoutUseCase logoutUseCase;

    @BeforeEach
    void setUp() {
        logoutUseCase = new LogoutUseCase(jwtTokenService, refreshTokenStore);
    }

    @Test
    void doesNothingWhenRefreshTokenIsNull() {
        // Act
        logoutUseCase.execute(null);

        // Assert
        verifyNoInteractions(jwtTokenService, refreshTokenStore);
    }

    @Test
    void doesNothingWhenRefreshTokenIsBlank() {
        // Act
        logoutUseCase.execute("   ");

        // Assert
        verifyNoInteractions(jwtTokenService, refreshTokenStore);
    }

    @Test
    void revokesJtiWhenRefreshTokenIsValid() {
        // Arrange
        String refreshToken = "valid.refresh.token";
        Claims claims = mock(Claims.class);
        when(jwtTokenService.parseToken(refreshToken, true)).thenReturn(claims);
        when(jwtTokenService.extractJti(claims)).thenReturn("refresh-jti");

        // Act
        logoutUseCase.execute(refreshToken);

        // Assert
        verify(refreshTokenStore).revoke("refresh-jti");
    }

    @Test
    void completesWithoutErrorWhenRefreshTokenIsInvalid() {
        // Arrange
        String refreshToken = "invalid.refresh.token";
        when(jwtTokenService.parseToken(refreshToken, true))
                .thenThrow(new InvalidCredentialsException("Token is invalid."));

        // Act
        logoutUseCase.execute(refreshToken);

        // Assert
        verify(refreshTokenStore, never()).revoke(anyString());
    }

    @Test
    void completesWithoutErrorWhenTokenParsingThrowsIllegalArgumentException() {
        // Arrange
        String refreshToken = "malformed.refresh.token";
        when(jwtTokenService.parseToken(refreshToken, true))
                .thenThrow(new IllegalArgumentException("Malformed token"));

        // Act
        logoutUseCase.execute(refreshToken);

        // Assert
        verify(refreshTokenStore, never()).revoke(anyString());
    }
}
