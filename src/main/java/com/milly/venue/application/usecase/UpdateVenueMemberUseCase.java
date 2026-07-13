package com.milly.venue.application.usecase;

import com.milly.common.application.exception.ResourceNotFoundException;
import com.milly.venue.application.dto.UpdateVenueMemberRequest;
import com.milly.venue.application.dto.VenueMemberResponse;
import com.milly.venue.application.port.outbound.UserProfilePort;
import com.milly.venue.application.port.outbound.UserProfilePort.UserProfileSummary;
import com.milly.venue.application.service.VenueAuthorizationService;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.MemberStatus;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateVenueMemberUseCase {

    private final VenueAuthorizationService venueAuthorizationService;
    private final VenueMembershipJpaRepository venueMembershipRepository;
    private final UserProfilePort userProfilePort;

    @Transactional
    public VenueMemberResponse execute(
            UUID venueId,
            UUID actorUserId,
            UUID memberId,
            UpdateVenueMemberRequest request) {
        if (request.role() == null && request.status() == null) {
            throw new IllegalArgumentException("At least one of role or status must be provided.");
        }

        VenueMembershipEntity actor = venueAuthorizationService.requireAtLeastRole(
                actorUserId, venueId, VenueRole.MANAGER);

        VenueMembershipEntity target = venueMembershipRepository.findByIdAndVenueId(memberId, venueId)
                .orElseThrow(ResourceNotFoundException::new);

        if (request.role() != null) {
            venueAuthorizationService.requireCanAssignRole(actor, target, request.role());
            target.changeRole(request.role());
        }

        if (request.status() != null) {
            if (request.status() == MemberStatus.INVITED) {
                throw new IllegalArgumentException("Membership status cannot be set to invited.");
            }
            venueAuthorizationService.requireCanManageMember(actor, target);
            if (request.status() == MemberStatus.ACTIVE) {
                target.activate();
            } else {
                target.deactivate();
            }
        }

        VenueMembershipEntity saved = venueMembershipRepository.save(target);
        UserProfileSummary profile = userProfilePort.findByIds(List.of(saved.getUserId()))
                .get(saved.getUserId());
        return VenueMemberResponse.of(saved, profile);
    }
}
