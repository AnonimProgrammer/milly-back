package com.milly.venue.application.dto;

import com.milly.venue.domain.valueobject.VenueRole;
import jakarta.validation.constraints.NotNull;

public record CreateVenueInvitationRequest(
        @NotNull(message = "Role is required.")
        VenueRole role
) {
}