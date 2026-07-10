package com.milly.order.application.polluter;

import com.milly.venue.application.polluter.ManagedVenue;

import java.util.UUID;

public record OrderTestFixture(
        ManagedVenue venue,
        UUID tableId,
        UUID menuItemId
) {
}