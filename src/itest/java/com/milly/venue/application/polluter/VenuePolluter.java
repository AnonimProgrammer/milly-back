package com.milly.venue.application.polluter;

import com.milly.auth.application.polluter.AuthSession;
import com.milly.auth.application.polluter.AuthSessionPolluter;
import com.milly.venue.application.dto.CreateVenueRequest;
import com.milly.venue.application.dto.CreateVenueResponse;
import com.milly.venue.application.usecase.AssignVenueMembershipUseCase;
import com.milly.venue.application.usecase.CreateVenueUseCase;
import com.milly.venue.domain.valueobject.VenueRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VenuePolluter {

    private final AuthSessionPolluter authSessionPolluter;
    private final CreateVenueUseCase createVenueUseCase;
    private final AssignVenueMembershipUseCase assignVenueMembershipUseCase;

    public ManagedVenue createManagedVenue() {
        AuthSession manager = authSessionPolluter.registerPasswordUser();
        CreateVenueResponse venue = createVenueUseCase.execute(
                manager.userId(),
                new CreateVenueRequest("Integration Test Venue", "Test City"));
        return new ManagedVenue(venue.id(), manager);
    }

    public AuthSession addMember(UUID venueId, VenueRole role) {
        AuthSession member = authSessionPolluter.registerPasswordUser();
        assignVenueMembershipUseCase.execute(venueId, member.userId(), role);
        return member;
    }
}