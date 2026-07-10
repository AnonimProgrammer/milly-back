package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.ContinueAuthRequest;
import com.milly.auth.domain.entity.UserAuthEntity;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.model.ExternalIdentity;
import com.milly.auth.domain.model.IdentityResolution;
import com.milly.auth.domain.valueobject.AuthProviderType;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserAuthJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserJpaRepository;
import com.milly.common.exception.InvalidCredentialsException;
import com.milly.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.milly.auth.application.usecase.builder.UserTestBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolveIdentityUseCaseTest {

    @Mock
    private UserAuthJpaRepository userAuthRepository;

    @Mock
    private UserJpaRepository userRepository;

    @Mock
    private CreateUserUseCase createUserUseCase;

    private ResolveIdentityUseCase resolveIdentityUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID providerId = UUID.randomUUID();
    private final ContinueAuthRequest.UserProfileDto profile = new ContinueAuthRequest.UserProfileDto(
            "Jane", "Doe", "jane.doe@example.com");

    @BeforeEach
    void setUp() {
        resolveIdentityUseCase = new ResolveIdentityUseCase(
                userAuthRepository, userRepository, createUserUseCase);
    }

    @Test
    void returnsExistingUserWhenAuthRecordFound() {
        // Arrange
        ExternalIdentity identity = new ExternalIdentity(
                AuthProviderType.PASSWORD, "jane.doe@example.com", "jane.doe@example.com");
        UserEntity existingUser = aUser().withId(userId).build();
        UserAuthEntity userAuth = UserAuthEntity.create(
                userId, providerId, "jane.doe@example.com", "jane.doe@example.com", "encoded-password");
        when(userAuthRepository.findByProviderTypeAndProviderUserId(
                AuthProviderType.PASSWORD, "jane.doe@example.com"))
                .thenReturn(Optional.of(userAuth));
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        // Act
        IdentityResolution resolution = resolveIdentityUseCase.execute(identity, profile, "secret-password");

        // Assert
        assertThat(resolution.user()).isSameAs(existingUser);
        assertThat(resolution.newUser()).isFalse();
        verifyNoInteractions(createUserUseCase);
    }

    @Test
    void createsUserWhenAuthRecordNotFound() {
        // Arrange
        ExternalIdentity identity = new ExternalIdentity(
                AuthProviderType.GOOGLE, "google-subject-123", "google.user@example.com");
        UserEntity createdUser = aUser().withId(userId).withEmail("google.user@example.com").build();
        when(userAuthRepository.findByProviderTypeAndProviderUserId(
                AuthProviderType.GOOGLE, "google-subject-123"))
                .thenReturn(Optional.empty());
        when(createUserUseCase.execute(identity, profile, "secret-password")).thenReturn(createdUser);

        // Act
        IdentityResolution resolution = resolveIdentityUseCase.execute(identity, profile, "secret-password");

        // Assert
        assertThat(resolution.user()).isSameAs(createdUser);
        assertThat(resolution.newUser()).isTrue();
        verify(createUserUseCase).execute(identity, profile, "secret-password");
        verifyNoInteractions(userRepository);
    }

    @Test
    void throwsInvalidCredentialsWhenPasswordProviderHasNoPasswordHash() {
        // Arrange
        ExternalIdentity identity = new ExternalIdentity(
                AuthProviderType.PASSWORD, "jane.doe@example.com", "jane.doe@example.com");
        UserAuthEntity userAuth = UserAuthEntity.create(
                userId, providerId, "jane.doe@example.com", "jane.doe@example.com", null);
        when(userAuthRepository.findByProviderTypeAndProviderUserId(
                AuthProviderType.PASSWORD, "jane.doe@example.com"))
                .thenReturn(Optional.of(userAuth));
        when(userRepository.findById(userId)).thenReturn(Optional.of(aUser().withId(userId).build()));

        // Act & Assert
        assertThatThrownBy(() -> resolveIdentityUseCase.execute(identity, profile, "secret-password"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password.");

        verifyNoInteractions(createUserUseCase);
    }

    @Test
    void allowsOAuthExistingUserWithoutPasswordHash() {
        // Arrange
        ExternalIdentity identity = new ExternalIdentity(
                AuthProviderType.GOOGLE, "google-subject-123", "google.user@example.com");
        UserEntity existingUser = aUser().withId(userId).withEmail("google.user@example.com").build();
        UserAuthEntity userAuth = UserAuthEntity.create(
                userId, providerId, "google-subject-123", "google.user@example.com", null);
        when(userAuthRepository.findByProviderTypeAndProviderUserId(
                AuthProviderType.GOOGLE, "google-subject-123"))
                .thenReturn(Optional.of(userAuth));
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        // Act
        IdentityResolution resolution = resolveIdentityUseCase.execute(identity, profile, null);

        // Assert
        assertThat(resolution.user()).isSameAs(existingUser);
        assertThat(resolution.newUser()).isFalse();
        verifyNoInteractions(createUserUseCase);
    }

    @Test
    void throwsNotFoundWhenUserAuthExistsButUserMissing() {
        // Arrange
        ExternalIdentity identity = new ExternalIdentity(
                AuthProviderType.GOOGLE, "google-subject-123", "google.user@example.com");
        UserAuthEntity userAuth = UserAuthEntity.create(
                userId, providerId, "google-subject-123", "google.user@example.com", null);
        when(userAuthRepository.findByProviderTypeAndProviderUserId(
                AuthProviderType.GOOGLE, "google-subject-123"))
                .thenReturn(Optional.of(userAuth));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> resolveIdentityUseCase.execute(identity, profile, null))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(createUserUseCase);
    }
}
