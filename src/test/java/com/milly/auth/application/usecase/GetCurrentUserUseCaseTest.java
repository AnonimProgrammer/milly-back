package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.CurrentUserResponse;
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
class GetCurrentUserUseCaseTest {

    @Mock
    private UserJpaRepository userRepository;

    @Mock
    private LoadAuthUserUseCase loadAuthUserUseCase;

    private GetCurrentUserUseCase getCurrentUserUseCase;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        getCurrentUserUseCase = new GetCurrentUserUseCase(userRepository, loadAuthUserUseCase);
    }

    @Test
    void returnsCurrentUserWithRoleNamesAsStrings() {
        // Arrange
        UserEntity user = aUser()
                .withId(userId)
                .withFirstName("Jane")
                .withLastName("Doe")
                .withEmail("jane.doe@example.com")
                .build();
        AuthUser authUser = new AuthUser(userId, List.of(RoleName.USER, RoleName.ADMIN));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(loadAuthUserUseCase.execute(user)).thenReturn(authUser);

        // Act
        CurrentUserResponse response = getCurrentUserUseCase.execute(userId);

        // Assert
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("jane.doe@example.com");
        assertThat(response.firstName()).isEqualTo("Jane");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.phoneNumber()).isNull();
        assertThat(response.roles()).containsExactly("USER", "ADMIN");
        verify(loadAuthUserUseCase).execute(user);
    }

    @Test
    void throwsNotFoundWhenUserDoesNotExist() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> getCurrentUserUseCase.execute(userId))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(loadAuthUserUseCase);
    }
}