package com.milly.venue.application.usecase;

import com.milly.common.application.exception.AccessDeniedException;
import com.milly.common.application.dto.PageResponse;
import com.milly.venue.application.dto.VenueMemberResponse;
import com.milly.venue.application.port.outbound.UserProfilePort;
import com.milly.venue.application.port.outbound.UserProfilePort.UserProfileSummary;
import com.milly.venue.application.service.VenueAuthorizationService;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.MemberListStatusFilter;
import com.milly.venue.domain.valueobject.MemberStatus;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListVenueMembersUseCaseTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    @Mock
    private VenueMembershipJpaRepository venueMembershipRepository;

    @Mock
    private UserProfilePort userProfilePort;

    private ListVenueMembersUseCase listVenueMembersUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID memberUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listVenueMembersUseCase = new ListVenueMembersUseCase(
                venueAuthorizationService, venueMembershipRepository, userProfilePort);
    }

    @Test
    void returnsPaginatedActiveVenueMembersForManager() {
        // Arrange
        VenueMembershipEntity membership = VenueMembershipEntity.create(venueId, memberUserId, VenueRole.EMPLOYEE);
        UserProfileSummary profile = new UserProfileSummary(
                memberUserId, "Sam", "Chen", "sam.chen@example.com", MemberStatus.ACTIVE);
        when(venueMembershipRepository.findByVenueWithFilters(
                eq(venueId), eq(MemberStatus.ACTIVE), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(membership), PageRequest.of(0, 1), 2));
        when(userProfilePort.findByIds(List.of(memberUserId))).thenReturn(Map.of(memberUserId, profile));

        // Act
        PageResponse<VenueMemberResponse> response = listVenueMembersUseCase.execute(
                venueId, userId, null, 1, MemberListStatusFilter.ACTIVE, null);

        // Assert
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().firstName()).isEqualTo("Sam");
        assertThat(response.data().getFirst().lastName()).isEqualTo("Chen");
        assertThat(response.data().getFirst().email()).isEqualTo("sam.chen@example.com");
        assertThat(response.data().getFirst().role()).isEqualTo(VenueRole.EMPLOYEE);
        assertThat(response.data().getFirst().status()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(response.pagination().limit()).isEqualTo(1);
        assertThat(response.pagination().hasNext()).isTrue();
        assertThat(response.pagination().nextCursor()).isEqualTo("1");
        verify(venueAuthorizationService).requireAtLeastRole(userId, venueId, VenueRole.MANAGER);
    }

    @Test
    void filtersMembersByRoleAndInactiveStatus() {
        // Arrange
        VenueMembershipEntity membership = VenueMembershipEntity.create(venueId, memberUserId, VenueRole.MANAGER);
        membership.deactivate();
        UserProfileSummary profile = new UserProfileSummary(
                memberUserId, "Sam", "Chen", "sam.chen@example.com", MemberStatus.ACTIVE);
        when(venueMembershipRepository.findByVenueWithFilters(
                eq(venueId), eq(MemberStatus.INACTIVE), eq(VenueRole.MANAGER), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(membership)));
        when(userProfilePort.findByIds(List.of(memberUserId))).thenReturn(Map.of(memberUserId, profile));

        // Act
        PageResponse<VenueMemberResponse> response = listVenueMembersUseCase.execute(
                venueId, userId, null, 20, MemberListStatusFilter.INACTIVE, VenueRole.MANAGER);

        // Assert
        assertThat(response.data()).singleElement()
                .satisfies(member -> {
                    assertThat(member.role()).isEqualTo(VenueRole.MANAGER);
                    assertThat(member.status()).isEqualTo(MemberStatus.INACTIVE);
                });
    }

    @Test
    void throwsAccessDeniedWhenUserIsNotManager() {
        // Arrange
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireAtLeastRole(userId, venueId, VenueRole.MANAGER);

        // Act & Assert
        assertThatThrownBy(() -> listVenueMembersUseCase.execute(
                venueId, userId, null, 20, MemberListStatusFilter.ACTIVE, null))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(venueMembershipRepository, userProfilePort);
    }

    @Test
    void throwsIllegalArgumentExceptionForInvalidCursor() {
        // Act & Assert
        assertThatThrownBy(() -> listVenueMembersUseCase.execute(
                venueId, userId, "invalid", 20, MemberListStatusFilter.ACTIVE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cursor must be a non-negative integer page token.");

        verify(venueAuthorizationService).requireAtLeastRole(userId, venueId, VenueRole.MANAGER);
        verifyNoInteractions(venueMembershipRepository, userProfilePort);
    }
}
