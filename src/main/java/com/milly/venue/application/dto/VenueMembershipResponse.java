package com.milly.venue.application.dto;

import com.milly.venue.domain.entity.VenueEntity;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;

import java.util.UUID;

public record VenueMembershipResponse(
        UUID venueId,
        String venueName,
        String location,
        VenueRole role
) {

    public static VenueMembershipResponse of(
            VenueEntity venue,
            VenueMembershipEntity membership) {
        return new VenueMembershipResponse(
                venue.getId(),
                venue.getName(),
                venue.getLocation(),
                membership.getRole()
        );
    }
}
