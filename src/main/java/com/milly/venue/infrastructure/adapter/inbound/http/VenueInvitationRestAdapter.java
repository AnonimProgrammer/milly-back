package com.milly.venue.infrastructure.adapter.inbound.http;

import com.milly.common.idempotency.Idempotent;
import com.milly.common.web.ApiResponse;
import com.milly.venue.application.dto.CreateVenueInvitationRequest;
import com.milly.venue.application.dto.CreateVenueInvitationResponse;
import com.milly.venue.application.usecase.CreateVenueInvitationUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues/{venueId}/invitations")
@RequiredArgsConstructor
public class VenueInvitationRestAdapter {

    private final CreateVenueInvitationUseCase createVenueInvitationUseCase;

    @Idempotent
    @PostMapping
    public ResponseEntity<ApiResponse<CreateVenueInvitationResponse>> createInvitation(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID venueId,
            @Valid @RequestBody CreateVenueInvitationRequest request) {
        CreateVenueInvitationResponse response =
                createVenueInvitationUseCase.execute(userId, venueId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Invitation created successfully."));
    }
}