package com.milly.venue.application.usecase;

import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignVenueMembershipUseCase {

    private final VenueMembershipJpaRepository venueMembershipRepository;

    @Transactional
    public VenueMembershipEntity execute(UUID venueId, UUID userId, VenueRole role) {
        VenueMembershipEntity membership = VenueMembershipEntity.create(venueId, userId, role);
        return venueMembershipRepository.save(membership);
    }
}
