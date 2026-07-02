package com.milly.venue.application.usecase;

import com.milly.venue.application.dto.VenueMembershipResponse;
import com.milly.venue.domain.entity.VenueEntity;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueJpaRepository;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListMyVenuesUseCase {

    private final VenueJpaRepository venueRepository;
    private final VenueMembershipJpaRepository venueMembershipRepository;

    @Transactional(readOnly = true)
    public List<VenueMembershipResponse> execute(UUID userId) {
        List<VenueMembershipEntity> memberships = venueMembershipRepository.findAllByUserId(userId);

        if (memberships.isEmpty()) {
            return List.of();
        }

        Map<UUID, VenueEntity> venuesById = venueRepository
                .findAllById(memberships.stream().map(VenueMembershipEntity::getVenueId).toList())
                .stream()
                .collect(Collectors.toMap(VenueEntity::getId, venue -> venue));

        return memberships.stream()
                .map(membership -> VenueMembershipResponse.of(venuesById.get(membership.getVenueId()), membership))
                .toList();
    }
}