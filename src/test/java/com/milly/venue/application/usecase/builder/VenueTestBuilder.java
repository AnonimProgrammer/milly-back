package com.milly.venue.application.usecase.builder;

import com.milly.venue.domain.entity.VenueEntity;
import com.milly.venue.domain.valueobject.VenueStatus;

import java.util.UUID;

public final class VenueTestBuilder {

    private UUID id = UUID.randomUUID();
    private String name = "Milly Bistro";
    private String location = "Barcelona, Spain";
    private VenueStatus status = VenueStatus.ACTIVE;

    private VenueTestBuilder() {
    }

    public static VenueTestBuilder aVenue() {
        return new VenueTestBuilder();
    }

    public VenueTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public VenueTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public VenueTestBuilder withLocation(String location) {
        this.location = location;
        return this;
    }

    public VenueEntity build() {
        VenueEntity venue = VenueEntity.create(name, location, status);
        venue.setId(id);
        return venue;
    }
}
