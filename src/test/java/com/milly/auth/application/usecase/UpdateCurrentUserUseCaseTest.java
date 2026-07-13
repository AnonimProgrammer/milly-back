package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.CurrentUserResponse;
import com.milly.auth.application.dto.UpdateCurrentUserRequest;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.model.AuthUser;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserJpaRepository;
import com.milly.common.application.exception.ResourceNotFoundException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateCurrentUserUseCaseTest {

    @Mock
    private UserJpaRepository userRepository;

    @Mock
    private LoadAuthUserUseCase loadAuthUserUseCase;

    private UpdateCurrentUserUseCase updateCurrentUserUseCase;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        updateCurrentUserUseCase = new UpdateCurrentUserUseCase(userRepository, loadAuthUserUseCase);
    }

    @Test
    void updatesProvidedProfileFieldsAndReturnsCurrentUser() {
        // Arrange
        UserEntity user = aUser()
                .withId(userId)
                .withFirstName("Jane")
                .withLastName("Doe")
                .withPhoneNumber("+994501112233")
                .build();
        AuthUser authUser = new AuthUser(userId, List.of(RoleName.USER));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(loadAuthUserUseCase.execute(user)).thenReturn(authUser);

        // Act
        CurrentUserResponse response = updateCurrentUserUseCase.execute(
                userId,
                new UpdateCurrentUserRequest("  Omar  ", "  Ismailov  ", "+994551234567"));

        // Assert
        assertThat(user.getFirstName()).isEqualTo("Omar");
        assertThat(user.getLastName()).isEqualTo("Ismailov");
        assertThat(user.getPhoneNumber()).isEqualTo("+994551234567");
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.firstName()).isEqualTo("Omar");
        assertThat(response.lastName()).isEqualTo("Ismailov");
        assertThat(response.phoneNumber()).isEqualTo("+994551234567");
        assertThat(response.roles()).containsExactly("USER");
        verify(userRepository).save(user);
    }

    @Test
    void updatesOnlyPhoneAndClearsBlankPhoneNumber() {
        // Arrange
        UserEntity user = aUser()
                .withId(userId)
                .withPhoneNumber("+994501112233")
                .build();
        AuthUser authUser = new AuthUser(userId, List.of(RoleName.USER));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(loadAuthUserUseCase.execute(user)).thenReturn(authUser);

        // Act
        CurrentUserResponse response = updateCurrentUserUseCase.execute(
                userId,
                new UpdateCurrentUserRequest(null, null, ""));

        // Assert
        assertThat(user.getFirstName()).isEqualTo("Jane");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getPhoneNumber()).isNull();
        assertThat(response.phoneNumber()).isNull();
    }

    @Test
    void rejectsBlankFirstName() {
        // Arrange
        UserEntity user = aUser().withId(userId).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> updateCurrentUserUseCase.execute(
                userId,
                new UpdateCurrentUserRequest("   ", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("First name must not be blank.");
    }

    @Test
    void requiresAtLeastOneField() {
        // Act & Assert
        assertThatThrownBy(() -> updateCurrentUserUseCase.execute(
                userId,
                new UpdateCurrentUserRequest(null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one of firstName, lastName, or phoneNumber must be provided.");

        verifyNoInteractions(userRepository, loadAuthUserUseCase);
    }

    @Test
    void throwsNotFoundWhenUserDoesNotExist() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> updateCurrentUserUseCase.execute(
                userId,
                new UpdateCurrentUserRequest("Omar", null, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(loadAuthUserUseCase);
    }
}
