package com.milly.venue.infrastructure.adapter.outbound.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.milly.venue.application.port.outbound.VenueInvitationStore;
import com.milly.venue.domain.model.VenueInvitation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CaffeineVenueInvitationStore implements VenueInvitationStore {

    private final Cache<UUID, VenueInvitation> venueInvitationCache;

    @Override
    public void register(VenueInvitation invitation) {
        venueInvitationCache.put(invitation.token(), invitation);
    }

    @Override
    public Optional<VenueInvitation> find(UUID token) {
        return Optional.ofNullable(venueInvitationCache.getIfPresent(token));
    }

    @Override
    public Optional<VenueInvitation> claim(UUID token) {
        VenueInvitation invitation = venueInvitationCache.asMap().remove(token);
        return Optional.ofNullable(invitation);
    }
}