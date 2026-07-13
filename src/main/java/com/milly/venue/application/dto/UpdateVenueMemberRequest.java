package com.milly.venue.application.dto;

import com.milly.venue.domain.valueobject.MemberStatus;
import com.milly.venue.domain.valueobject.VenueRole;

public record UpdateVenueMemberRequest(
        VenueRole role,
        MemberStatus status) {
}
