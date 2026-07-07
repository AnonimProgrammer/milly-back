package com.milly.venue.application.port.outbound;

import com.milly.venue.domain.model.VenueInvitation;

import java.util.Optional;
import java.util.UUID;

public interface VenueInvitationStore {

    void register(VenueInvitation invitation);

    Optional<VenueInvitation> find(UUID token);

    Optional<VenueInvitation> claim(UUID token);
}
