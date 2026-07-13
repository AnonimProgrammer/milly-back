package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.AdminUserResponse;
import com.milly.auth.application.dto.UpdateAdminUserRequest;
import com.milly.auth.application.usecase.builder.UserTestBuilder;
import com.milly.auth.domain.entity.RoleEntity;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.entity.UserRoleEntity;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.domain.valueobject.UserStatus;
import com.milly.auth.infrastructure.adapter.outbound.persistence.RoleJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserRoleJpaRepository;
import com.milly.common.application.exception.AccessDeniedException;
import com.milly.common.application.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateAdminUserUseCaseTest {

    @Mock
    private UserJpaRepository userRepository;

    @Mock
    private UserRoleJpaRepository userRoleRepository;

    @Mock
    private RoleJpaRepository roleRepository;

    private UpdateAdminUserUseCase updateAdminUserUseCase;

    private final UUID actorId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        updateAdminUserUseCase = new UpdateAdminUserUseCase(
                userRepository, userRoleRepository, roleRepository);
    }

    @Test
    void updatesStatusAndPreservesExistingRoles() {
        // Arrange
        UserEntity target = UserTestBuilder.aUser().withId(targetId).build();
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);
        when(userRoleRepository.findRoleNamesByUserId(targetId)).thenReturn(List.of(RoleName.USER));

        // Act
        AdminUserResponse response = updateAdminUserUseCase.execute(
                actorId,
                targetId,
                new UpdateAdminUserRequest(UserStatus.SUSPENDED, null));

        // Assert
        assertThat(target.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(response.status()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(response.roles()).containsExactly("USER");
        verify(userRoleRepository, never()).deleteByUserId(any());
    }

    @Test
    void replacesRolesAndAlwaysKeepsUserRole() {
        // Arrange
        UserEntity target = UserTestBuilder.aUser().withId(targetId).build();
        RoleEntity userRole = role(RoleName.USER);
        RoleEntity adminRole = role(RoleName.ADMIN);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));

        // Act
        AdminUserResponse response = updateAdminUserUseCase.execute(
                actorId,
                targetId,
                new UpdateAdminUserRequest(null, List.of(RoleName.ADMIN)));

        // Assert
        verify(userRoleRepository).deleteByUserId(targetId);
        ArgumentCaptor<UserRoleEntity> roleCaptor = ArgumentCaptor.forClass(UserRoleEntity.class);
        verify(userRoleRepository, times(2)).save(roleCaptor.capture());
        assertThat(roleCaptor.getAllValues())
                .extracting(UserRoleEntity::getRoleId)
                .containsExactlyInAnyOrder(userRole.getId(), adminRole.getId());
        assertThat(response.roles()).containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    void rejectsSelfDeactivation() {
        // Arrange
        UserEntity target = UserTestBuilder.aUser().withId(actorId).build();
        when(userRepository.findById(actorId)).thenReturn(Optional.of(target));

        // Act & Assert
        assertThatThrownBy(() -> updateAdminUserUseCase.execute(
                actorId,
                actorId,
                new UpdateAdminUserRequest(UserStatus.INACTIVE, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsSelfAdminRemoval() {
        // Arrange
        UserEntity target = UserTestBuilder.aUser().withId(actorId).build();
        when(userRepository.findById(actorId)).thenReturn(Optional.of(target));

        // Act & Assert
        assertThatThrownBy(() -> updateAdminUserUseCase.execute(
                actorId,
                actorId,
                new UpdateAdminUserRequest(null, List.of(RoleName.USER))))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void throwsWhenUserMissing() {
        // Arrange
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> updateAdminUserUseCase.execute(
                actorId,
                targetId,
                new UpdateAdminUserRequest(UserStatus.ACTIVE, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private RoleEntity role(RoleName name) {
        RoleEntity role = org.mockito.Mockito.mock(RoleEntity.class);
        when(role.getId()).thenReturn(UUID.randomUUID());
        return role;
    }
}
