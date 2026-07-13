package com.milly.venue.application.usecase;

import com.milly.common.application.exception.AccessDeniedException;
import com.milly.common.application.exception.ResourceNotFoundException;
import com.milly.venue.application.dto.UpdateVenueMemberRequest;
import com.milly.venue.application.dto.VenueMemberResponse;
import com.milly.venue.application.port.outbound.UserProfilePort;
import com.milly.venue.application.port.outbound.UserProfilePort.UserProfileSummary;
import com.milly.venue.application.service.VenueAuthorizationService;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.MemberStatus;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateVenueMemberUseCaseTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    @Mock
    private VenueMembershipJpaRepository venueMembershipRepository;

    @Mock
    private UserProfilePort userProfilePort;

    private UpdateVenueMemberUseCase updateVenueMemberUseCase;

    private final UUID venueId = UUID.randomUUID();
    private final UUID actorUserId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();
    private final UUID memberUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        updateVenueMemberUseCase = new UpdateVenueMemberUseCase(
                venueAuthorizationService, venueMembershipRepository, userProfilePort);
    }

    @Test
    void managerBlocksEmployee() {
        // Arrange
        VenueMembershipEntity actor = membership(actorUserId, VenueRole.MANAGER);
        VenueMembershipEntity target = membership(memberUserId, VenueRole.EMPLOYEE);
        target.setId(memberId);
        UserProfileSummary profile = new UserProfileSummary(
                memberUserId, "Sam", "Chen", "sam.chen@example.com", MemberStatus.ACTIVE);

        when(venueAuthorizationService.requireAtLeastRole(actorUserId, venueId, VenueRole.MANAGER))
                .thenReturn(actor);
        when(venueMembershipRepository.findByIdAndVenueId(memberId, venueId))
                .thenReturn(Optional.of(target));
        when(venueMembershipRepository.save(target)).thenReturn(target);
        when(userProfilePort.findByIds(List.of(memberUserId))).thenReturn(Map.of(memberUserId, profile));

        // Act
        VenueMemberResponse response = updateVenueMemberUseCase.execute(
                venueId,
                actorUserId,
                memberId,
                new UpdateVenueMemberRequest(null, MemberStatus.INACTIVE));

        // Assert
        assertThat(response.status()).isEqualTo(MemberStatus.INACTIVE);
        assertThat(response.role()).isEqualTo(VenueRole.EMPLOYEE);
        verify(venueAuthorizationService).requireCanManageMember(actor, target);
    }

    @Test
    void managerPromotesEmployeeToManager() {
        // Arrange
        VenueMembershipEntity actor = membership(actorUserId, VenueRole.MANAGER);
        VenueMembershipEntity target = membership(memberUserId, VenueRole.EMPLOYEE);
        target.setId(memberId);
        UserProfileSummary profile = new UserProfileSummary(
                memberUserId, "Sam", "Chen", "sam.chen@example.com", MemberStatus.ACTIVE);

        when(venueAuthorizationService.requireAtLeastRole(actorUserId, venueId, VenueRole.MANAGER))
                .thenReturn(actor);
        when(venueMembershipRepository.findByIdAndVenueId(memberId, venueId))
                .thenReturn(Optional.of(target));
        when(venueMembershipRepository.save(target)).thenReturn(target);
        when(userProfilePort.findByIds(List.of(memberUserId))).thenReturn(Map.of(memberUserId, profile));

        // Act
        VenueMemberResponse response = updateVenueMemberUseCase.execute(
                venueId,
                actorUserId,
                memberId,
                new UpdateVenueMemberRequest(VenueRole.MANAGER, null));

        // Assert
        assertThat(response.role()).isEqualTo(VenueRole.MANAGER);
        verify(venueAuthorizationService).requireCanAssignRole(actor, target, VenueRole.MANAGER);
    }

    @Test
    void managerCannotUpdateAnotherManager() {
        // Arrange
        VenueMembershipEntity actor = membership(actorUserId, VenueRole.MANAGER);
        VenueMembershipEntity target = membership(memberUserId, VenueRole.MANAGER);
        target.setId(memberId);

        when(venueAuthorizationService.requireAtLeastRole(actorUserId, venueId, VenueRole.MANAGER))
                .thenReturn(actor);
        when(venueMembershipRepository.findByIdAndVenueId(memberId, venueId))
                .thenReturn(Optional.of(target));
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireCanManageMember(actor, target);

        // Act & Assert
        assertThatThrownBy(() -> updateVenueMemberUseCase.execute(
                venueId,
                actorUserId,
                memberId,
                new UpdateVenueMemberRequest(null, MemberStatus.INACTIVE)))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(userProfilePort);
        verify(venueMembershipRepository, never()).save(any());
    }

    @Test
    void throwsNotFoundWhenMemberDoesNotExist() {
        // Arrange
        when(venueAuthorizationService.requireAtLeastRole(actorUserId, venueId, VenueRole.MANAGER))
                .thenReturn(membership(actorUserId, VenueRole.OWNER));
        when(venueMembershipRepository.findByIdAndVenueId(memberId, venueId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> updateVenueMemberUseCase.execute(
                venueId,
                actorUserId,
                memberId,
                new UpdateVenueMemberRequest(null, MemberStatus.INACTIVE)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsRequestWithoutRoleOrStatus() {
        // Act & Assert
        assertThatThrownBy(() -> updateVenueMemberUseCase.execute(
                venueId, actorUserId, memberId, new UpdateVenueMemberRequest(null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one of role or status");

        verifyNoInteractions(venueAuthorizationService, venueMembershipRepository, userProfilePort);
    }

    private VenueMembershipEntity membership(UUID userId, VenueRole role) {
        return VenueMembershipEntity.create(venueId, userId, role);
    }
}
