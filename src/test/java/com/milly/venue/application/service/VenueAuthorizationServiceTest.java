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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void managerRoleIsAllowed() {
        when(venueMembershipRepository.findByUserIdAndVenueId(userId, venueId))
                .thenReturn(Optional.of(membership(VenueRole.MANAGER)));

        assertDoesNotThrow(() -> venueAuthorizationService.requireRole(userId, venueId, VenueRole.MANAGER));
    }

    @Test
    void waiterRoleIsDenied() {
        when(venueMembershipRepository.findByUserIdAndVenueId(userId, venueId))
                .thenReturn(Optional.of(membership(VenueRole.WAITER)));

        assertThrows(AccessDeniedException.class,
                () -> venueAuthorizationService.requireRole(userId, venueId, VenueRole.MANAGER));
    }

    @Test
    void missingMembershipIsDenied() {
        when(venueMembershipRepository.findByUserIdAndVenueId(userId, venueId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> venueAuthorizationService.requireRole(userId, venueId, VenueRole.MANAGER));
    }

    private VenueMembershipEntity membership(VenueRole role) {
        return VenueMembershipEntity.create(venueId, userId, role);
    }
}
