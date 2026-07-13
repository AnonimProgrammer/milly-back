package com.milly.venue.infrastructure.adapter.inbound.http;

import com.milly.common.application.dto.ApiResponse;
import com.milly.common.application.dto.PageResponse;
import com.milly.venue.application.dto.UpdateVenueMemberRequest;
import com.milly.venue.application.dto.VenueMemberResponse;
import com.milly.venue.application.usecase.ListVenueMembersUseCase;
import com.milly.venue.application.usecase.UpdateVenueMemberUseCase;
import com.milly.venue.domain.valueobject.MemberListStatusFilter;
import com.milly.venue.domain.valueobject.VenueRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/venues/{venueId}/members")
public class VenueMemberRestAdapter {

    private final ListVenueMembersUseCase listVenueMembersUseCase;
    private final UpdateVenueMemberUseCase updateVenueMemberUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VenueMemberResponse>>> listMembers(
            @PathVariable UUID venueId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "active") String status,
            @RequestParam(required = false) VenueRole role,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(
                listVenueMembersUseCase.execute(
                        venueId,
                        userId,
                        cursor,
                        limit,
                        MemberListStatusFilter.fromValue(status),
                        role),
                "Venue members retrieved successfully."));
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<ApiResponse<VenueMemberResponse>> updateMember(
            @PathVariable UUID venueId,
            @PathVariable UUID memberId,
            @RequestBody UpdateVenueMemberRequest request,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(
                updateVenueMemberUseCase.execute(venueId, userId, memberId, request),
                "Venue member updated successfully."));
    }
}
