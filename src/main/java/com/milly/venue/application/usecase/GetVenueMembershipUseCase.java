package com.milly.venue.application.usecase;

import com.milly.venue.application.dto.VenueMembershipResponse;
import com.milly.venue.domain.entity.VenueEntity;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueJpaRepository;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetVenueMembershipUseCase {

    private static final String ACCESS_DENIED_MESSAGE = "You do not have access to this venue.";

    private final VenueJpaRepository venueRepository;
    private final VenueMembershipJpaRepository venueMembershipRepository;

    @Transactional(readOnly = true)
    public VenueMembershipResponse execute(UUID venueId, UUID userId) {
        VenueMembershipEntity membership = venueMembershipRepository
                .findByUserIdAndVenueId( userId,venueId)
                .orElseThrow(() -> new AccessDeniedException(ACCESS_DENIED_MESSAGE));
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new AccessDeniedException(ACCESS_DENIED_MESSAGE));

        return VenueMembershipResponse.of(venue, membership);
    }
}
