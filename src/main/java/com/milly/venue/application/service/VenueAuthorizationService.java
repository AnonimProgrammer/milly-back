package com.milly.venue.application.service;

import com.milly.common.application.exception.AccessDeniedException;
import com.milly.common.application.exception.InactiveMembershipException;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.MemberStatus;
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

    public VenueMembershipEntity requireActiveMember(UUID userId, UUID venueId) {
        VenueMembershipEntity membership = venueMembershipRepository.findByUserIdAndVenueId(userId, venueId)
                .orElseThrow(AccessDeniedException::new);
        if (membership.getStatus() != MemberStatus.ACTIVE) {
            throw new InactiveMembershipException();
        }
        return membership;
    }

    public VenueMembershipEntity requireAtLeastRole(UUID userId, UUID venueId, VenueRole minimumRole) {
        VenueMembershipEntity membership = requireActiveMember(userId, venueId);
        if (!membership.getRole().isAtLeast(minimumRole)) {
            throw new AccessDeniedException();
        }
        return membership;
    }

    public void requireCanManageMember(VenueMembershipEntity actor, VenueMembershipEntity target) {
        if (actor.getId().equals(target.getId())) {
            throw new AccessDeniedException();
        }

        if (target.getRole() == VenueRole.OWNER) {
            throw new AccessDeniedException();
        }

        if (actor.getRole() == VenueRole.OWNER) {
            return;
        }

        if (actor.getRole() == VenueRole.MANAGER && target.getRole() == VenueRole.EMPLOYEE) {
            return;
        }

        throw new AccessDeniedException();
    }

    public void requireCanAssignRole(
            VenueMembershipEntity actor,
            VenueMembershipEntity target,
            VenueRole newRole) {
        if (newRole == VenueRole.OWNER) {
            throw new AccessDeniedException();
        }

        requireCanManageMember(actor, target);
    }
}
