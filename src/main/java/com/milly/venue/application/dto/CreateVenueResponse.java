package com.milly.venue.application.dto;

import com.milly.venue.domain.entity.VenueEntity;
import com.milly.venue.domain.valueobject.VenueRole;

import java.util.UUID;

public record CreateVenueResponse(
        UUID id,
        String name,
        String location,
        VenueRole role
) {

    public static CreateVenueResponse of(VenueEntity venue, VenueRole role) {
        return new CreateVenueResponse(
                venue.getId(),
                venue.getName(),
                venue.getLocation(),
                role
        );
    }
}
