package com.milly.venue.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RedeemVenueInvitationRequest(
        @NotNull(message = "Token is required.")
        UUID token
) {
}