package com.milly.venue.application.usecase;

import com.milly.venue.application.dto.CreateVenueRequest;
import com.milly.venue.application.dto.CreateVenueResponse;
import com.milly.venue.domain.entity.VenueEntity;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.domain.valueobject.VenueStatus;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateVenueUseCase {

    private final VenueJpaRepository venueRepository;
    private final AssignVenueMembershipUseCase assignVenueMembershipUseCase;

    @Transactional
    public CreateVenueResponse execute(UUID userId, CreateVenueRequest request) {
        VenueEntity venue = VenueEntity.create(request.name(), request.location(), VenueStatus.ACTIVE);
        VenueEntity savedVenue = venueRepository.save(venue);

        VenueMembershipEntity membership = assignVenueMembershipUseCase.execute(
                savedVenue.getId(), userId, VenueRole.OWNER);

        return CreateVenueResponse.of(savedVenue, membership.getRole());
    }
}
