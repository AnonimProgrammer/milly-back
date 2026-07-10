package com.milly.auth.application.usecase;

import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.model.AuthUser;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserRoleJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.milly.auth.application.usecase.builder.UserTestBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoadAuthUserUseCaseTest {

    @Mock
    private UserRoleJpaRepository userRoleRepository;

    private LoadAuthUserUseCase loadAuthUserUseCase;

    @BeforeEach
    void setUp() {
        loadAuthUserUseCase = new LoadAuthUserUseCase(userRoleRepository);
    }

    @Test
    void mapsUserToAuthUserWithRolesFromRepository() {
        // Arrange
        UserEntity user = aUser().build();
        when(userRoleRepository.findRoleNamesByUserId(user.getId()))
                .thenReturn(List.of(RoleName.USER, RoleName.ADMIN));

        // Act
        AuthUser authUser = loadAuthUserUseCase.execute(user);

        // Assert
        assertThat(authUser.id()).isEqualTo(user.getId());
        assertThat(authUser.roles()).containsExactly(RoleName.USER, RoleName.ADMIN);
        verify(userRoleRepository).findRoleNamesByUserId(user.getId());
    }

    @Test
    void returnsAuthUserWithEmptyRolesWhenUserHasNoRoles() {
        // Arrange
        UserEntity user = aUser().build();
        when(userRoleRepository.findRoleNamesByUserId(user.getId())).thenReturn(List.of());

        // Act
        AuthUser authUser = loadAuthUserUseCase.execute(user);

        // Assert
        assertThat(authUser.id()).isEqualTo(user.getId());
        assertThat(authUser.roles()).isEmpty();
    }
}