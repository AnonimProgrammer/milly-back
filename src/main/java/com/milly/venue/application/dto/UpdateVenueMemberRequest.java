package com.milly.venue.application.dto;

import com.milly.venue.domain.valueobject.MemberStatus;
import com.milly.venue.domain.valueobject.VenueRole;
import jakarta.validation.constraints.AssertTrue;

public record UpdateVenueMemberRequest(
        VenueRole role,
        MemberStatus status) {

    @AssertTrue(message = "At least one of role or status must be provided.")
    public boolean isAtLeastOneFieldPresent() {
        return role != null || status != null;
    }
}
