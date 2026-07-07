package com.milly.venue.infrastructure.adapter.inbound.http;

import com.milly.common.web.ApiResponse;
import com.milly.venue.application.dto.RedeemVenueInvitationRequest;
import com.milly.venue.application.dto.VenueMembershipResponse;
import com.milly.venue.application.usecase.RedeemVenueInvitationUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
public class InvitationRestAdapter {

    private final RedeemVenueInvitationUseCase redeemVenueInvitationUseCase;

    @PostMapping("/redeem")
    public ResponseEntity<ApiResponse<VenueMembershipResponse>> redeemInvitation(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody RedeemVenueInvitationRequest request) {
        VenueMembershipResponse response = redeemVenueInvitationUseCase.execute(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Invitation redeemed successfully."));
    }
}
