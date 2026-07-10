package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.ContinueAuthRequest;
import com.milly.auth.application.dto.ContinueAuthResponse;
import com.milly.auth.application.port.outbound.AuthProvider;
import com.milly.auth.application.port.outbound.RefreshTokenStore;
import com.milly.auth.application.usecase.factory.AuthProviderFactory;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.model.AuthUser;
import com.milly.auth.domain.model.ExternalIdentity;
import com.milly.auth.domain.model.IdentityResolution;
import com.milly.auth.domain.model.IssuedRefreshToken;
import com.milly.auth.domain.valueobject.AuthProviderType;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.infrastructure.adapter.outbound.security.JwtTokenService;
import com.milly.common.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.milly.auth.application.usecase.builder.UserTestBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContinueAuthUseCaseTest {

    @Mock
    private AuthProviderFactory providerFactory;

    @Mock
    private ResolveIdentityUseCase resolveIdentityUseCase;

    @Mock
    private LoadAuthUserUseCase loadAuthUserUseCase;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private AuthProvider authProvider;

    @Captor
    private ArgumentCaptor<ExternalIdentity> identityCaptor;

    private ContinueAuthUseCase continueAuthUseCase;

    private final UUID userId = UUID.randomUUID();
    private final String accessToken = "issued.access.token";
    private final String refreshTokenValue = "issued.refresh.token";
    private final String refreshJti = "refresh-jti";
    private final ContinueAuthRequest.UserProfileDto profile = new ContinueAuthRequest.UserProfileDto(
            "Jane", "Doe", "jane.doe@example.com");
    private final ExternalIdentity identity = new ExternalIdentity(
            AuthProviderType.PASSWORD, "jane.doe@example.com", "jane.doe@example.com");

    @BeforeEach
    void setUp() {
        continueAuthUseCase = new ContinueAuthUseCase(
                providerFactory,
                resolveIdentityUseCase,
                loadAuthUserUseCase,
                jwtTokenService,
                refreshTokenStore);
    }

    @Test
    void returnsTokensAndFlagsNewUserForFirstTimeSignIn() {
        // Arrange
        ContinueAuthRequest request = passwordRequest("secret-password");
        UserEntity user = aUser().withId(userId).build();
        AuthUser authUser = new AuthUser(userId, List.of(RoleName.USER));
        IssuedRefreshToken issuedRefreshToken = new IssuedRefreshToken(refreshTokenValue, refreshJti);
        stubSuccessfulAuthFlow(user, authUser, issuedRefreshToken, new IdentityResolution(user, true));

        // Act
        ContinueAuthResponse response = continueAuthUseCase.execute(request);

        // Assert
        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.refreshToken()).isEqualTo(refreshTokenValue);
        assertThat(response.newUser()).isTrue();
        verify(refreshTokenStore).register(refreshJti, userId);
        verify(resolveIdentityUseCase).execute(identity, profile, "secret-password");
    }

    @Test
    void returnsTokensAndFlagsExistingUserForReturningSignIn() {
        // Arrange
        ContinueAuthRequest request = googleRequest();
        UserEntity user = aUser().withId(userId).withEmail("google.user@example.com").build();
        AuthUser authUser = new AuthUser(userId, List.of(RoleName.USER, RoleName.ADMIN));
        IssuedRefreshToken issuedRefreshToken = new IssuedRefreshToken(refreshTokenValue, refreshJti);
        ExternalIdentity googleIdentity = new ExternalIdentity(
                AuthProviderType.GOOGLE, "google-subject-123", "google.user@example.com");
        when(providerFactory.get(AuthProviderType.GOOGLE)).thenReturn(authProvider);
        when(authProvider.authenticate(request.credentials())).thenReturn(googleIdentity);
        when(resolveIdentityUseCase.execute(googleIdentity, profile, null))
                .thenReturn(new IdentityResolution(user, false));
        when(loadAuthUserUseCase.execute(user)).thenReturn(authUser);
        when(jwtTokenService.issueAccessToken(authUser)).thenReturn(accessToken);
        when(jwtTokenService.issueRefreshToken(authUser)).thenReturn(issuedRefreshToken);

        // Act
        ContinueAuthResponse response = continueAuthUseCase.execute(request);

        // Assert
        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.refreshToken()).isEqualTo(refreshTokenValue);
        assertThat(response.newUser()).isFalse();
        verify(refreshTokenStore).register(refreshJti, userId);
        verify(resolveIdentityUseCase).execute(googleIdentity, profile, null);
    }

    @Test
    void passesAuthenticatedIdentityAndOptionalPasswordToResolveIdentity() {
        // Arrange
        ContinueAuthRequest request = passwordRequest("secret-password");
        UserEntity user = aUser().withId(userId).build();
        AuthUser authUser = new AuthUser(userId, List.of(RoleName.USER));
        IssuedRefreshToken issuedRefreshToken = new IssuedRefreshToken(refreshTokenValue, refreshJti);
        stubSuccessfulAuthFlow(user, authUser, issuedRefreshToken, new IdentityResolution(user, false));

        // Act
        continueAuthUseCase.execute(request);

        // Assert
        verify(authProvider).authenticate(request.credentials());
        verify(resolveIdentityUseCase).execute(identityCaptor.capture(), eq(profile), eq("secret-password"));
        assertThat(identityCaptor.getValue()).isEqualTo(identity);
    }

    @Test
    void propagatesFailureFromProviderAuthentication() {
        // Arrange
        ContinueAuthRequest request = passwordRequest("secret-password");
        when(providerFactory.get(AuthProviderType.PASSWORD)).thenReturn(authProvider);
        when(authProvider.authenticate(request.credentials()))
                .thenThrow(new InvalidCredentialsException("Invalid username or password."));

        // Act & Assert
        assertThatThrownBy(() -> continueAuthUseCase.execute(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password.");

        verifyNoInteractions(resolveIdentityUseCase, loadAuthUserUseCase, jwtTokenService, refreshTokenStore);
    }

    @Test
    void propagatesFailureFromUnsupportedProvider() {
        // Arrange
        ContinueAuthRequest request = passwordRequest("secret-password");
        when(providerFactory.get(AuthProviderType.PASSWORD))
                .thenThrow(new UnsupportedOperationException("Unsupported auth provider: PASSWORD"));

        // Act & Assert
        assertThatThrownBy(() -> continueAuthUseCase.execute(request))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Unsupported auth provider: PASSWORD");

        verifyNoInteractions(authProvider, resolveIdentityUseCase, loadAuthUserUseCase, jwtTokenService, refreshTokenStore);
    }

    @Test
    void propagatesFailureFromResolveIdentity() {
        // Arrange
        ContinueAuthRequest request = passwordRequest("secret-password");
        when(providerFactory.get(AuthProviderType.PASSWORD)).thenReturn(authProvider);
        when(authProvider.authenticate(request.credentials())).thenReturn(identity);
        when(resolveIdentityUseCase.execute(identity, profile, "secret-password"))
                .thenThrow(new InvalidCredentialsException("Profile data is required for first-time sign-in."));

        // Act & Assert
        assertThatThrownBy(() -> continueAuthUseCase.execute(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Profile data is required for first-time sign-in.");

        verifyNoInteractions(loadAuthUserUseCase, jwtTokenService, refreshTokenStore);
    }

    @Test
    void doesNotIssueTokensWhenResolveIdentityFails() {
        // Arrange
        ContinueAuthRequest request = googleRequest();
        ExternalIdentity googleIdentity = new ExternalIdentity(
                AuthProviderType.GOOGLE, "google-subject-123", "google.user@example.com");
        when(providerFactory.get(AuthProviderType.GOOGLE)).thenReturn(authProvider);
        when(authProvider.authenticate(request.credentials())).thenReturn(googleIdentity);
        when(resolveIdentityUseCase.execute(googleIdentity, profile, null))
                .thenThrow(new InvalidCredentialsException("Invalid username or password."));

        // Act & Assert
        assertThatThrownBy(() -> continueAuthUseCase.execute(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoMoreInteractions(authProvider);
        verifyNoInteractions(loadAuthUserUseCase, jwtTokenService, refreshTokenStore);
    }

    private void stubSuccessfulAuthFlow(
            UserEntity user,
            AuthUser authUser,
            IssuedRefreshToken issuedRefreshToken,
            IdentityResolution resolution) {
        when(providerFactory.get(AuthProviderType.PASSWORD)).thenReturn(authProvider);
        when(authProvider.authenticate(passwordRequest("secret-password").credentials())).thenReturn(identity);
        when(resolveIdentityUseCase.execute(identity, profile, "secret-password")).thenReturn(resolution);
        when(loadAuthUserUseCase.execute(user)).thenReturn(authUser);
        when(jwtTokenService.issueAccessToken(authUser)).thenReturn(accessToken);
        when(jwtTokenService.issueRefreshToken(authUser)).thenReturn(issuedRefreshToken);
    }

    private ContinueAuthRequest passwordRequest(String password) {
        return new ContinueAuthRequest(
                AuthProviderType.PASSWORD,
                Map.of(
                        "username", "jane.doe@example.com",
                        "password", password),
                profile);
    }

    private ContinueAuthRequest googleRequest() {
        return new ContinueAuthRequest(
                AuthProviderType.GOOGLE,
                Map.of("idToken", "google-id-token"),
                profile);
    }
}