package com.milly.venue.infrastructure.adapter.inbound.http;

import com.milly.common.web.ApiResponse;
import com.milly.venue.application.dto.CreateVenueRequest;
import com.milly.venue.application.dto.CreateVenueResponse;
import com.milly.venue.application.dto.VenueMembershipResponse;
import com.milly.venue.application.usecase.CreateVenueUseCase;
import com.milly.venue.application.usecase.GetVenueMembershipUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class VenueRestAdapter {

    private final CreateVenueUseCase createVenueUseCase;
    private final GetVenueMembershipUseCase getVenueMembershipUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateVenueResponse>> createVenue(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateVenueRequest request) {
        CreateVenueResponse response = createVenueUseCase.execute(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Venue created successfully."));
    }

    @GetMapping("/{id}/me")
    public ResponseEntity<ApiResponse<VenueMembershipResponse>> getVenueMembership(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {
        VenueMembershipResponse response = getVenueMembershipUseCase.execute(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Venue membership retrieved successfully."));
    }
}
