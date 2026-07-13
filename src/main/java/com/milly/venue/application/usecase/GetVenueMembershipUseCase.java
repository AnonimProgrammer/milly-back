package com.milly.venue.application.usecase;

import com.milly.common.application.exception.ResourceNotFoundException;
import com.milly.venue.application.dto.VenueMembershipResponse;
import com.milly.venue.application.service.VenueAuthorizationService;
import com.milly.venue.domain.entity.VenueEntity;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetVenueMembershipUseCase {

    private final VenueJpaRepository venueRepository;
    private final VenueAuthorizationService venueAuthorizationService;

    @Transactional(readOnly = true)
    public VenueMembershipResponse execute(UUID venueId, UUID userId) {
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(ResourceNotFoundException::new);

        VenueMembershipEntity membership = venueAuthorizationService.requireActiveMember(userId, venueId);

        return VenueMembershipResponse.of(venue, membership);
    }
}
