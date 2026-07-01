package com.milly.venue.application.usecase;

import com.milly.venue.application.dto.VenueMembershipResponse;
import com.milly.venue.domain.entity.VenueEntity;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueJpaRepository;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetVenueMembershipUseCaseTests {

    @Mock
    private VenueJpaRepository venueRepository;

    @Mock
    private VenueMembershipJpaRepository venueMembershipRepository;

    @InjectMocks
    private GetVenueMembershipUseCase getVenueMembershipUseCase;

    @ParameterizedTest
    @EnumSource(VenueRole.class)
    void executeReturnsVenueAndActualMembershipRole(VenueRole role) {
        UUID userId = UUID.randomUUID();
        VenueEntity venue = VenueEntity.createActive("Milly Bistro", "Barcelona, Spain");
        VenueMembershipEntity membership = VenueMembershipEntity.create(
                venue.getId(), userId, role);
        when(venueMembershipRepository.findByVenueIdAndUserId(venue.getId(), userId))
                .thenReturn(Optional.of(membership));
        when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));

        VenueMembershipResponse response = getVenueMembershipUseCase.execute(venue.getId(), userId);

        assertThat(response.venueId()).isEqualTo(venue.getId());
        assertThat(response.venueName()).isEqualTo("Milly Bistro");
        assertThat(response.location()).isEqualTo("Barcelona, Spain");
        assertThat(response.role()).isEqualTo(role);
    }

    @Test
    void executeThrowsAccessDeniedWhenMembershipDoesNotExist() {
        UUID venueId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(venueMembershipRepository.findByVenueIdAndUserId(venueId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> getVenueMembershipUseCase.execute(venueId, userId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You do not have access to this venue.");

        verifyNoInteractions(venueRepository);
    }
}
