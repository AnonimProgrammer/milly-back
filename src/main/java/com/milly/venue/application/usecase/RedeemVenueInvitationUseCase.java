package com.milly.venue.application.usecase;

import com.milly.common.application.exception.InvalidInvitationException;
import com.milly.common.application.exception.ResourceNotFoundException;
import com.milly.common.application.exception.VenueMembershipAlreadyExistsException;
import com.milly.venue.application.dto.RedeemVenueInvitationRequest;
import com.milly.venue.application.dto.VenueMembershipResponse;
import com.milly.venue.application.port.outbound.VenueInvitationStore;
import com.milly.venue.domain.entity.VenueEntity;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.model.VenueInvitation;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueJpaRepository;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedeemVenueInvitationUseCase {

    private final VenueInvitationStore venueInvitationStore;
    private final VenueMembershipJpaRepository venueMembershipRepository;
    private final VenueJpaRepository venueRepository;
    private final AssignVenueMembershipUseCase assignVenueMembershipUseCase;

    @Transactional
    public VenueMembershipResponse execute(UUID userId, RedeemVenueInvitationRequest request) {
        VenueInvitation invitation = venueInvitationStore.find(request.token())
                .orElseThrow(InvalidInvitationException::new);

        if (venueMembershipRepository.findByUserIdAndVenueId(userId, invitation.venueId()).isPresent()) {
            throw new VenueMembershipAlreadyExistsException();
        }

        VenueInvitation claimed = venueInvitationStore.claim(request.token())
                .orElseThrow(InvalidInvitationException::new);

        VenueEntity venue = venueRepository.findById(claimed.venueId())
                .orElseThrow(ResourceNotFoundException::new);

        VenueMembershipEntity membership = assignVenueMembershipUseCase.execute(
                claimed.venueId(),
                userId,
                claimed.role());

        return VenueMembershipResponse.of(venue, membership);
    }
}