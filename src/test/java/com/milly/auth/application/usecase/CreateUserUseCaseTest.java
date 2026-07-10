package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.ContinueAuthRequest;
import com.milly.auth.domain.entity.AuthProviderEntity;
import com.milly.auth.domain.entity.RoleEntity;
import com.milly.auth.domain.entity.UserAuthEntity;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.entity.UserRoleEntity;
import com.milly.auth.domain.model.ExternalIdentity;
import com.milly.auth.domain.valueobject.AuthProviderType;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.infrastructure.adapter.outbound.persistence.AuthProviderJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.persistence.RoleJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserAuthJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserRoleJpaRepository;
import com.milly.common.exception.InvalidCredentialsException;
import com.milly.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {

    @Mock
    private UserJpaRepository userRepository;

    @Mock
    private UserAuthJpaRepository userAuthRepository;

    @Mock
    private AuthProviderJpaRepository authProviderRepository;

    @Mock
    private RoleJpaRepository roleRepository;

    @Mock
    private UserRoleJpaRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Captor
    private ArgumentCaptor<UserEntity> userCaptor;

    @Captor
    private ArgumentCaptor<UserAuthEntity> userAuthCaptor;

    @Captor
    private ArgumentCaptor<UserRoleEntity> userRoleCaptor;

    private CreateUserUseCase createUserUseCase;

    private final UUID providerId = UUID.randomUUID();
    private final UUID roleId = UUID.randomUUID();
    private final UUID savedUserId = UUID.randomUUID();
    private final ContinueAuthRequest.UserProfileDto profile = new ContinueAuthRequest.UserProfileDto(
            "Jane", "Doe", "jane.doe@example.com");

    @BeforeEach
    void setUp() {
        createUserUseCase = new CreateUserUseCase(
                userRepository,
                userAuthRepository,
                authProviderRepository,
                roleRepository,
                userRoleRepository,
                passwordEncoder);
    }

    @Test
    void createsPasswordUserWithEncodedPasswordAndDefaultRole() {
        // Arrange
        ExternalIdentity identity = new ExternalIdentity(AuthProviderType.PASSWORD, "jane.doe@example.com", null);
        stubProviderAndRole(AuthProviderType.PASSWORD);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(savedUserId);
            return user;
        });
        when(passwordEncoder.encode("secret-password")).thenReturn("encoded-password");

        // Act
        UserEntity savedUser = createUserUseCase.execute(identity, profile, "secret-password");

        // Assert
        assertThat(savedUser.getId()).isEqualTo(savedUserId);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getFirstName()).isEqualTo("Jane");
        assertThat(userCaptor.getValue().getLastName()).isEqualTo("Doe");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("jane.doe@example.com");

        verify(userRoleRepository).save(userRoleCaptor.capture());
        assertThat(userRoleCaptor.getValue().getUserId()).isEqualTo(savedUserId);
        assertThat(userRoleCaptor.getValue().getRoleId()).isEqualTo(roleId);

        verify(userAuthRepository).save(userAuthCaptor.capture());
        assertThat(userAuthCaptor.getValue().getUserId()).isEqualTo(savedUserId);
        assertThat(userAuthCaptor.getValue().getProviderId()).isEqualTo(providerId);
        assertThat(userAuthCaptor.getValue().getProviderUserId()).isEqualTo("jane.doe@example.com");
        assertThat(userAuthCaptor.getValue().getEmail()).isEqualTo("jane.doe@example.com");
        assertThat(userAuthCaptor.getValue().getPasswordHash()).isEqualTo("encoded-password");
    }

    @Test
    void createsOAuthUserWithoutPasswordHash() {
        // Arrange
        ExternalIdentity identity = new ExternalIdentity(
                AuthProviderType.GOOGLE, "google-subject-123", "google.user@example.com");
        ContinueAuthRequest.UserProfileDto oauthProfile = new ContinueAuthRequest.UserProfileDto(
                "Jane", "Doe", "google.user@example.com");
        stubProviderAndRole(AuthProviderType.GOOGLE);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(savedUserId);
            return user;
        });

        // Act
        createUserUseCase.execute(identity, oauthProfile, null);

        // Assert
        verify(userAuthRepository).save(userAuthCaptor.capture());
        assertThat(userAuthCaptor.getValue().getPasswordHash()).isNull();
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void usesIdentityEmailWhenProfileEmailIsBlank() {
        // Arrange
        ExternalIdentity identity = new ExternalIdentity(
                AuthProviderType.GOOGLE, "google-subject-123", "Identity@Example.com");
        ContinueAuthRequest.UserProfileDto profileWithoutEmail = new ContinueAuthRequest.UserProfileDto(
                "Jane", "Doe", "   ");
        stubProviderAndRole(AuthProviderType.GOOGLE);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(savedUserId);
            return user;
        });

        // Act
        createUserUseCase.execute(identity, profileWithoutEmail, null);

        // Assert
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("identity@example.com");
        verify(userAuthRepository).save(userAuthCaptor.capture());
        assertThat(userAuthCaptor.getValue().getEmail()).isEqualTo("identity@example.com");
    }

    @Test
    void throwsWhenProfileIsNull() {
        // Arrange
        ExternalIdentity identity = new ExternalIdentity(AuthProviderType.PASSWORD, "jane.doe@example.com", null);

        // Act & Assert
        assertThatThrownBy(() -> createUserUseCase.execute(identity, null, "secret-password"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Profile data is required for first-time sign-in.");

        verifyNoInteractions(
                authProviderRepository,
                roleRepository,
                userRepository,
                userRoleRepository,
                userAuthRepository,
                passwordEncoder);
    }

    @Test
    void throwsWhenPasswordIsMissingForPasswordProvider() {
        // Arrange
        ExternalIdentity identity = new ExternalIdentity(AuthProviderType.PASSWORD, "jane.doe@example.com", null);
        AuthProviderEntity provider = mock(AuthProviderEntity.class);
        RoleEntity role = mock(RoleEntity.class);
        when(role.getId()).thenReturn(roleId);
        when(authProviderRepository.findByType(AuthProviderType.PASSWORD)).thenReturn(Optional.of(provider));
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(role));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(savedUserId);
            return user;
        });

        // Act & Assert
        assertThatThrownBy(() -> createUserUseCase.execute(identity, profile, "   "))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Password is required for password registration.");

        verify(userRepository).save(any(UserEntity.class));
        verify(userRoleRepository).save(any(UserRoleEntity.class));
        verifyNoInteractions(userAuthRepository, passwordEncoder);
    }

    @Test
    void throwsWhenRequiredProfileFieldIsMissing() {
        // Arrange
        ExternalIdentity identity = new ExternalIdentity(AuthProviderType.GOOGLE, "google-subject-123", null);
        ContinueAuthRequest.UserProfileDto incompleteProfile = new ContinueAuthRequest.UserProfileDto(
                " ", "Doe", "jane.doe@example.com");
        AuthProviderEntity provider = mock(AuthProviderEntity.class);
        RoleEntity role = mock(RoleEntity.class);
        when(authProviderRepository.findByType(AuthProviderType.GOOGLE)).thenReturn(Optional.of(provider));
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(role));

        // Act & Assert
        assertThatThrownBy(() -> createUserUseCase.execute(identity, incompleteProfile, null))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Missing required field: firstName");

        verifyNoInteractions(userRepository, userRoleRepository, userAuthRepository, passwordEncoder);
    }

    @Test
    void throwsWhenAuthProviderNotFound() {
        // Arrange
        ExternalIdentity identity = new ExternalIdentity(AuthProviderType.PASSWORD, "jane.doe@example.com", null);
        when(authProviderRepository.findByType(AuthProviderType.PASSWORD)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> createUserUseCase.execute(identity, profile, "secret-password"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(authProviderRepository).findByType(AuthProviderType.PASSWORD);
        verifyNoMoreInteractions(authProviderRepository);
        verifyNoInteractions(roleRepository, userRepository, userRoleRepository, userAuthRepository, passwordEncoder);
    }

    @Test
    void throwsWhenDefaultRoleNotFound() {
        // Arrange
        ExternalIdentity identity = new ExternalIdentity(AuthProviderType.PASSWORD, "jane.doe@example.com", null);
        AuthProviderEntity provider = mock(AuthProviderEntity.class);
        when(authProviderRepository.findByType(AuthProviderType.PASSWORD)).thenReturn(Optional.of(provider));
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> createUserUseCase.execute(identity, profile, "secret-password"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(roleRepository).findByName(RoleName.USER);
        verifyNoInteractions(userRepository, userRoleRepository, userAuthRepository, passwordEncoder);
    }

    private void stubProviderAndRole(AuthProviderType providerType) {
        AuthProviderEntity provider = mock(AuthProviderEntity.class);
        RoleEntity role = mock(RoleEntity.class);
        when(provider.getId()).thenReturn(providerId);
        when(role.getId()).thenReturn(roleId);
        when(authProviderRepository.findByType(providerType)).thenReturn(Optional.of(provider));
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(role));
    }
}
