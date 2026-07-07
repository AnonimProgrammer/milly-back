package com.milly.venue.application.polluter;

import com.milly.auth.application.polluter.AuthSession;

import java.util.UUID;

public record ManagedVenue(
        UUID venueId,
        AuthSession manager
) {
}
