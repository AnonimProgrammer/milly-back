package com.milly.venue.infrastructure.adapter.inbound.http;

import com.milly.common.web.ApiResponse;
import com.milly.venue.application.dto.CreateVenueRequest;
import com.milly.venue.application.dto.CreateVenueResponse;
import com.milly.venue.application.usecase.CreateVenueUseCase;
import com.milly.venue.domain.entity.VenueEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class VenueController {

    private final CreateVenueUseCase createVenueUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateVenueResponse>> createVenue(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateVenueRequest request) {
        VenueEntity venue = createVenueUseCase.execute(userId, request.name(), request.location());
        CreateVenueResponse response = CreateVenueResponse.of(venue, VenueRole.MANAGER);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Venue created successfully."));
    }
}
