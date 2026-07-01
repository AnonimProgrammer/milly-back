package com.milly.venue.application.usecase;

import com.milly.venue.domain.entity.VenueEntity;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueJpaRepository;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateVenueUseCase {

    private final VenueJpaRepository venueRepository;
    private final VenueMembershipJpaRepository venueMembershipRepository;

    @Transactional
    public VenueEntity execute(UUID userId, String name, String location) {
        VenueEntity venue = VenueEntity.createActive(name, location);
        VenueEntity savedVenue = venueRepository.save(venue);

        VenueMembershipEntity membership = VenueMembershipEntity.create(
                savedVenue.getId(), userId, VenueRole.MANAGER);
        venueMembershipRepository.save(membership);

        return savedVenue;
    }
}
