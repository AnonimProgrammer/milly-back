package com.milly.venue.application.dto;

import com.milly.venue.application.port.outbound.UserProfilePort.UserProfileSummary;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.MemberStatus;
import com.milly.venue.domain.valueobject.VenueRole;

import java.util.UUID;

public record VenueMemberResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        VenueRole role,
        MemberStatus status) {

    public static VenueMemberResponse of(VenueMembershipEntity membership, UserProfileSummary profile) {
        return new VenueMemberResponse(
                membership.getId(),
                profile.firstName(),
                profile.lastName(),
                profile.email(),
                membership.getRole(),
                membership.getStatus());
    }
}
