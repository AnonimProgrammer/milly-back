package com.milly.table.application.polluter;

import com.milly.venue.application.polluter.ManagedVenue;

import java.util.UUID;

public record TableTestFixture(
        ManagedVenue venue,
        UUID tableId
) {
}