package com.milly.venue.application.service;

import com.milly.common.exception.AccessDeniedException;
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

    @Test
    void returnsMembershipWhenUserIsMember() {
        VenueMembershipEntity membership = membership(VenueRole.WAITER);
        when(venueMembershipRepository.findByUserIdAndVenueId(userId, venueId))
                .thenReturn(Optional.of(membership));

        VenueMembershipEntity result = venueAuthorizationService.requireMember(userId, venueId);

        assertThat(result).isSameAs(membership);
        verify(venueMembershipRepository).findByUserIdAndVenueId(userId, venueId);
    }

    @Test
    void throwsAccessDeniedWhenRequiredMembershipIsMissing() {
        when(venueMembershipRepository.findByUserIdAndVenueId(userId, venueId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> venueAuthorizationService.requireMember(userId, venueId))
                .isInstanceOf(AccessDeniedException.class);

        verify(venueMembershipRepository).findByUserIdAndVenueId(userId, venueId);
    }

    @Test
    void managerRoleIsAllowed() {
        when(venueMembershipRepository.findByUserIdAndVenueId(userId, venueId))
                .thenReturn(Optional.of(membership(VenueRole.MANAGER)));

        assertDoesNotThrow(() -> venueAuthorizationService.requireRole(userId, venueId, VenueRole.MANAGER));

        verify(venueMembershipRepository).findByUserIdAndVenueId(userId, venueId);
    }

    @Test
    void waiterRoleIsDenied() {
        when(venueMembershipRepository.findByUserIdAndVenueId(userId, venueId))
                .thenReturn(Optional.of(membership(VenueRole.WAITER)));

        assertThrows(AccessDeniedException.class,
                () -> venueAuthorizationService.requireRole(userId, venueId, VenueRole.MANAGER));

        verify(venueMembershipRepository).findByUserIdAndVenueId(userId, venueId);
    }

    @Test
    void missingMembershipIsDenied() {
        when(venueMembershipRepository.findByUserIdAndVenueId(userId, venueId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> venueAuthorizationService.requireRole(userId, venueId, VenueRole.MANAGER));

        verify(venueMembershipRepository).findByUserIdAndVenueId(userId, venueId);
    }

    private VenueMembershipEntity membership(VenueRole role) {
        return VenueMembershipEntity.create(venueId, userId, role);
    }
}
