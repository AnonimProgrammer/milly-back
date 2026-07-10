package com.milly.auth.application.usecase;

import com.milly.auth.application.port.outbound.RefreshTokenStore;
import com.milly.auth.application.port.outbound.SessionTokenPort;
import com.milly.auth.domain.model.ParsedRefreshToken;
import com.milly.common.application.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseTest {

    @Mock
    private SessionTokenPort sessionTokenPort;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    private LogoutUseCase logoutUseCase;

    @BeforeEach
    void setUp() {
        logoutUseCase = new LogoutUseCase(sessionTokenPort, refreshTokenStore);
    }

    @Test
    void doesNothingWhenRefreshTokenIsNull() {
        // Act
        logoutUseCase.execute(null);

        // Assert
        verifyNoInteractions(sessionTokenPort, refreshTokenStore);
    }

    @Test
    void doesNothingWhenRefreshTokenIsBlank() {
        // Act
        logoutUseCase.execute("   ");

        // Assert
        verifyNoInteractions(sessionTokenPort, refreshTokenStore);
    }

    @Test
    void revokesJtiWhenRefreshTokenIsValid() {
        // Arrange
        String refreshToken = "valid.refresh.token";
        when(sessionTokenPort.parseRefreshToken(refreshToken))
                .thenReturn(new ParsedRefreshToken(UUID.randomUUID(), "refresh-jti"));

        // Act
        logoutUseCase.execute(refreshToken);

        // Assert
        verify(refreshTokenStore).revoke("refresh-jti");
    }

    @Test
    void completesWithoutErrorWhenRefreshTokenIsInvalid() {
        // Arrange
        String refreshToken = "invalid.refresh.token";
        when(sessionTokenPort.parseRefreshToken(refreshToken))
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
        when(sessionTokenPort.parseRefreshToken(refreshToken))
                .thenThrow(new IllegalArgumentException("Malformed token"));

        // Act
        logoutUseCase.execute(refreshToken);

        // Assert
        verify(refreshTokenStore, never()).revoke(anyString());
    }
}