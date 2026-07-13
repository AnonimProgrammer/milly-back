package com.milly.venue.application.usecase.builder;

import com.milly.venue.domain.model.VenueInvitation;
import com.milly.venue.domain.valueobject.VenueRole;

import java.util.UUID;

public final class VenueInvitationTestBuilder {

    private UUID token = UUID.randomUUID();
    private UUID venueId = UUID.randomUUID();
    private VenueRole role = VenueRole.EMPLOYEE;
    private UUID createdByUserId = UUID.randomUUID();

    private VenueInvitationTestBuilder() {
    }

    public static VenueInvitationTestBuilder aVenueInvitation() {
        return new VenueInvitationTestBuilder();
    }

    public VenueInvitationTestBuilder withToken(UUID token) {
        this.token = token;
        return this;
    }

    public VenueInvitationTestBuilder withVenueId(UUID venueId) {
        this.venueId = venueId;
        return this;
    }

    public VenueInvitationTestBuilder withRole(VenueRole role) {
        this.role = role;
        return this;
    }

    public VenueInvitationTestBuilder withCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
        return this;
    }

    public VenueInvitation build() {
        return new VenueInvitation(token, venueId, role, createdByUserId);
    }
}
