package com.milly.venue.application.usecase;

import com.milly.common.exception.ResourceNotFoundException;
import com.milly.venue.application.dto.CreateVenueInvitationRequest;
import com.milly.venue.application.dto.CreateVenueInvitationResponse;
import com.milly.venue.application.port.outbound.VenueInvitationStore;
import com.milly.venue.application.service.VenueAuthorizationService;
import com.milly.venue.application.service.VenueInvitationUrlBuilder;
import com.milly.venue.domain.model.VenueInvitation;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateVenueInvitationUseCase {

    private final VenueAuthorizationService venueAuthorizationService;
    private final VenueJpaRepository venueRepository;
    private final VenueInvitationStore venueInvitationStore;
    private final VenueInvitationUrlBuilder venueInvitationUrlBuilder;

    public CreateVenueInvitationResponse execute(
            UUID userId, UUID venueId, CreateVenueInvitationRequest request) {
        venueAuthorizationService.requireRole(userId, venueId, VenueRole.MANAGER);
        venueRepository.findById(venueId).orElseThrow(ResourceNotFoundException::new);

        UUID token = UUID.randomUUID();
        VenueInvitation invitation = new VenueInvitation(
                token,
                venueId,
                request.role(),
                userId);
        venueInvitationStore.register(invitation);

        return new CreateVenueInvitationResponse(
                token,
                venueInvitationUrlBuilder.build(token),
                request.role());
    }
}
