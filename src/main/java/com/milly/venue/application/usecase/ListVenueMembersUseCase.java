package com.milly.venue.application.usecase;

import com.milly.common.application.dto.PageResponse;
import com.milly.common.application.dto.PaginationMeta;
import com.milly.venue.application.dto.VenueMemberResponse;
import com.milly.venue.application.port.outbound.UserProfilePort;
import com.milly.venue.application.port.outbound.UserProfilePort.UserProfileSummary;
import com.milly.venue.application.service.VenueAuthorizationService;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListVenueMembersUseCase {

    private final VenueAuthorizationService venueAuthorizationService;
    private final VenueMembershipJpaRepository venueMembershipRepository;
    private final UserProfilePort userProfilePort;

    @Transactional(readOnly = true)
    public PageResponse<VenueMemberResponse> execute(
            UUID venueId,
            UUID userId,
            String cursor,
            int limit) {
        venueAuthorizationService.requireRole(userId, venueId, VenueRole.MANAGER);

        int safeLimit = Math.max(1, limit);
        int page = parseCursor(cursor);
        Pageable pageable = PageRequest.of(page, safeLimit);

        Page<VenueMembershipEntity> memberships = venueMembershipRepository
                .findAllByVenueIdOrderByCreatedAtAsc(venueId, pageable);

        List<VenueMembershipEntity> membershipContent = memberships.getContent();
        Map<UUID, UserProfileSummary> profilesByUserId = userProfilePort.findByIds(
                membershipContent.stream().map(VenueMembershipEntity::getUserId).toList());

        List<VenueMemberResponse> response = membershipContent.stream()
                .map(membership -> VenueMemberResponse.of(
                        membership,
                        profilesByUserId.get(membership.getUserId())))
                .toList();

        return new PageResponse<>(response, new PaginationMeta(
                nextCursor(memberships),
                previousCursor(memberships),
                memberships.hasNext(),
                memberships.hasPrevious(),
                safeLimit));
    }

    private int parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }

        try {
            return Math.max(0, Integer.parseInt(cursor));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Cursor must be a non-negative integer page token.", ex);
        }
    }

    private String nextCursor(Page<VenueMembershipEntity> memberships) {
        return memberships.hasNext() ? Integer.toString(memberships.getNumber() + 1) : null;
    }

    private String previousCursor(Page<VenueMembershipEntity> memberships) {
        return memberships.hasPrevious() ? Integer.toString(memberships.getNumber() - 1) : null;
    }
}
