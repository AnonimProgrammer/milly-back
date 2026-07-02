package com.milly.venue.application.service;

import com.milly.common.exception.AccessDeniedException;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VenueAuthorizationService {

    private final VenueMembershipJpaRepository venueMembershipRepository;

    public void requireManager(UUID userId, UUID venueId) {
        VenueMembershipEntity membership = venueMembershipRepository.findByUserIdAndVenueId(userId, venueId)
                .orElseThrow(AccessDeniedException::new);

        if (membership.getRole() != VenueRole.MANAGER) {
            throw new AccessDeniedException();
        }
    }
}
