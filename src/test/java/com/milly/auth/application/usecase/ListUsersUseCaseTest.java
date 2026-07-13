package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.AdminUserResponse;
import com.milly.auth.application.usecase.builder.UserTestBuilder;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.domain.valueobject.UserStatus;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserRoleJpaRepository;
import com.milly.common.application.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListUsersUseCaseTest {

    @Mock
    private UserJpaRepository userRepository;

    @Mock
    private UserRoleJpaRepository userRoleRepository;

    private ListUsersUseCase listUsersUseCase;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listUsersUseCase = new ListUsersUseCase(userRepository, userRoleRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsPaginatedUsersWithRoles() {
        // Arrange
        UserEntity user = UserTestBuilder.aUser()
                .withId(userId)
                .withFirstName("Sam")
                .withLastName("Chen")
                .withEmail("sam.chen@example.com")
                .build();
        user.setPhoneNumber("+994501112233");
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 1), 2));
        when(userRoleRepository.findRoleNamesByUserIds(List.of(userId)))
                .thenReturn(List.<Object[]>of(new Object[]{userId, RoleName.USER}));

        // Act
        PageResponse<AdminUserResponse> response = listUsersUseCase.execute(
                null, 1, null, null, null, null, null, null, null);

        // Assert
        assertThat(response.data()).hasSize(1);
        AdminUserResponse item = response.data().getFirst();
        assertThat(item.id()).isEqualTo(userId);
        assertThat(item.firstName()).isEqualTo("Sam");
        assertThat(item.lastName()).isEqualTo("Chen");
        assertThat(item.email()).isEqualTo("sam.chen@example.com");
        assertThat(item.phoneNumber()).isEqualTo("+994501112233");
        assertThat(item.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(item.roles()).containsExactly("USER");
        assertThat(response.pagination().limit()).isEqualTo(1);
        assertThat(response.pagination().hasNext()).isTrue();
        assertThat(response.pagination().nextCursor()).isEqualTo("1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void appliesFiltersAndTrimsBlankSearchValues() {
        // Arrange
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        PageResponse<AdminUserResponse> response = listUsersUseCase.execute(
                null,
                20,
                UserStatus.ACTIVE,
                RoleName.ADMIN,
                "  sam@example.com  ",
                "  +994  ",
                "  Sam  ",
                null,
                null);

        // Assert
        assertThat(response.data()).isEmpty();
        verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        verifyNoInteractions(userRoleRepository);
    }

    @Test
    void throwsIllegalArgumentExceptionForInvalidCursor() {
        // Arrange
        String invalidCursor = "invalid";

        // Act & Assert
        assertThatThrownBy(() -> listUsersUseCase.execute(
                invalidCursor, 20, null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cursor must be a non-negative integer page token.");
        verifyNoInteractions(userRepository, userRoleRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void usesCursorPageToken() {
        // Arrange
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 20), 0));

        // Act
        listUsersUseCase.execute("2", 20, null, null, null, null, null, null, null);

        // Assert
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
