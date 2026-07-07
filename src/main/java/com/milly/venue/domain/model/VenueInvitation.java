package com.milly.venue.domain.model;

import com.milly.venue.domain.valueobject.VenueRole;

import java.util.UUID;

public record VenueInvitation(
        UUID token,
        UUID venueId,
        VenueRole role,
        UUID createdByUserId
) {
}
