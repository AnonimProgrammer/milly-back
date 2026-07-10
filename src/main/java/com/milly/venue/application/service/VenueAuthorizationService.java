package com.milly.venue.application.service;

import com.milly.common.application.exception.AccessDeniedException;
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

    public VenueMembershipEntity requireMember(UUID userId, UUID venueId) {
        return venueMembershipRepository.findByUserIdAndVenueId(userId, venueId)
                .orElseThrow(AccessDeniedException::new);
    }

    public void requireRole(UUID userId, UUID venueId, VenueRole role) {
        VenueMembershipEntity membership = requireMember(userId, venueId);

        if (membership.getRole() != role) {
            throw new AccessDeniedException();
        }
    }
}
