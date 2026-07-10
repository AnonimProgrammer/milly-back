package com.milly.venue.application.dto;

import com.milly.venue.domain.valueobject.VenueRole;

import java.util.UUID;

public record CreateVenueInvitationResponse(
        UUID token,
        String inviteUrl,
        VenueRole role
) {
}