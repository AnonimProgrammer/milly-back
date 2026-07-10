package com.milly.venue.application.port.outbound;

import com.milly.venue.domain.valueobject.MemberStatus;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface UserProfilePort {

    Map<UUID, UserProfileSummary> findByIds(Collection<UUID> userIds);

    record UserProfileSummary(
            UUID id,
            String firstName,
            String lastName,
            String email,
            MemberStatus status) {
    }
}
