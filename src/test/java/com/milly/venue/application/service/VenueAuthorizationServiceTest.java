package com.milly.venue.application.service;

import com.milly.common.application.exception.AccessDeniedException;
import com.milly.common.application.exception.InactiveMembershipException;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueAuthorizationServiceTest {

    @Mock
    private VenueMembershipJpaRepository venueMembershipRepository;

    @InjectMocks
    private VenueAuthorizationService venueAuthorizationService;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID targetUserId = UUID.randomUUID();

    @Test
    void returnsMembershipWhenUserIsMember() {
        // Arrange
        VenueMembershipEntity membership = membership(userId, VenueRole.EMPLOYEE);
        when(venueMembershipRepository.findByUserIdAndVenueId(userId, venueId))
                .thenReturn(Optional.of(membership));

        // Act
        VenueMembershipEntity result = venueAuthorizationService.requireMember(userId, venueId);

        // Assert
        assertThat(result).isSameAs(membership);
        verify(venueMembershipRepository).findByUserIdAndVenueId(userId, venueId);
    }

    @Test
    void throwsAccessDeniedWhenRequiredMembershipIsMissing() {
        // Arrange
        when(venueMembershipRepository.findByUserIdAndVenueId(userId, venueId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> venueAuthorizationService.requireMember(userId, venueId))
                .isInstanceOf(AccessDeniedException.class);

        verify(venueMembershipRepository).findByUserIdAndVenueId(userId, venueId);
    }

    @Test
    void inactiveMemberIsDeniedForActiveMembershipCheck() {
        // Arrange
        VenueMembershipEntity membership = membership(userId, VenueRole.EMPLOYEE);
        membership.deactivate();
        when(venueMembershipRepository.findByUserIdAndVenueId(userId, venueId))
                .thenReturn(Optional.of(membership));

        // Act & Assert
        assertThrows(InactiveMembershipException.class,
                () -> venueAuthorizationService.requireActiveMember(userId, venueId));
    }

    @Test
    void managerRoleIsAllowedForAtLeastManagerCheck() {
        // Arrange
        when(venueMembershipRepository.findByUserIdAndVenueId(userId, venueId))
                .thenReturn(Optional.of(membership(userId, VenueRole.MANAGER)));

        // Act & Assert
        assertDoesNotThrow(() -> venueAuthorizationService.requireAtLeastRole(userId, venueId, VenueRole.MANAGER));
    }

    @Test
    void ownerRoleIsAllowedForAtLeastManagerCheck() {
        // Arrange
        when(venueMembershipRepository.findByUserIdAndVenueId(userId, venueId))
                .thenReturn(Optional.of(membership(userId, VenueRole.OWNER)));

        // Act & Assert
        assertDoesNotThrow(() -> venueAuthorizationService.requireAtLeastRole(userId, venueId, VenueRole.MANAGER));
    }

    @Test
    void employeeRoleIsDeniedForAtLeastManagerCheck() {
        // Arrange
        when(venueMembershipRepository.findByUserIdAndVenueId(userId, venueId))
                .thenReturn(Optional.of(membership(userId, VenueRole.EMPLOYEE)));

        // Act & Assert
        assertThrows(AccessDeniedException.class,
                () -> venueAuthorizationService.requireAtLeastRole(userId, venueId, VenueRole.MANAGER));
    }

    @Test
    void managerCanManageEmployee() {
        // Arrange
        VenueMembershipEntity actor = membership(userId, VenueRole.MANAGER);
        VenueMembershipEntity target = membership(targetUserId, VenueRole.EMPLOYEE);

        // Act & Assert
        assertDoesNotThrow(() -> venueAuthorizationService.requireCanManageMember(actor, target));
    }

    @Test
    void managerCannotManageAnotherManager() {
        // Arrange
        VenueMembershipEntity actor = membership(userId, VenueRole.MANAGER);
        VenueMembershipEntity target = membership(targetUserId, VenueRole.MANAGER);

        // Act & Assert
        assertThrows(AccessDeniedException.class,
                () -> venueAuthorizationService.requireCanManageMember(actor, target));
    }

    @Test
    void ownerCanManageManager() {
        // Arrange
        VenueMembershipEntity actor = membership(userId, VenueRole.OWNER);
        VenueMembershipEntity target = membership(targetUserId, VenueRole.MANAGER);

        // Act & Assert
        assertDoesNotThrow(() -> venueAuthorizationService.requireCanManageMember(actor, target));
    }

    @Test
    void ownerCannotManageAnotherOwner() {
        // Arrange
        VenueMembershipEntity actor = membership(userId, VenueRole.OWNER);
        VenueMembershipEntity target = membership(targetUserId, VenueRole.OWNER);

        // Act & Assert
        assertThrows(AccessDeniedException.class,
                () -> venueAuthorizationService.requireCanManageMember(actor, target));
    }

    @Test
    void cannotAssignOwnerRole() {
        // Arrange
        VenueMembershipEntity actor = membership(userId, VenueRole.OWNER);
        VenueMembershipEntity target = membership(targetUserId, VenueRole.EMPLOYEE);

        // Act & Assert
        assertThrows(AccessDeniedException.class,
                () -> venueAuthorizationService.requireCanAssignRole(actor, target, VenueRole.OWNER));
    }

    private VenueMembershipEntity membership(UUID memberUserId, VenueRole role) {
        return VenueMembershipEntity.create(venueId, memberUserId, role);
    }
}
