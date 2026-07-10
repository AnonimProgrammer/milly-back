package com.milly.venue.infrastructure.adapter.inbound.http;

import com.milly.common.application.dto.ApiResponse;
import com.milly.common.application.dto.PageResponse;
import com.milly.venue.application.dto.VenueMemberResponse;
import com.milly.venue.application.usecase.ListVenueMembersUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/venues/{venueId}/members")
public class VenueMemberRestAdapter {

    private final ListVenueMembersUseCase listVenueMembersUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VenueMemberResponse>>> listMembers(
            @PathVariable UUID venueId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(
                listVenueMembersUseCase.execute(venueId, userId, cursor, limit),
                "Venue members retrieved successfully."));
    }
}
