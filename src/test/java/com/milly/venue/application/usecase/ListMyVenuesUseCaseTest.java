package com.milly.venue.application.usecase;

import com.milly.venue.application.dto.VenueMembershipResponse;
import com.milly.venue.application.usecase.builder.VenueTestBuilder;
import com.milly.venue.domain.entity.VenueEntity;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueJpaRepository;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListMyVenuesUseCaseTest {

    @Mock
    private VenueJpaRepository venueRepository;

    @Mock
    private VenueMembershipJpaRepository venueMembershipRepository;

    private ListMyVenuesUseCase listMyVenuesUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID anotherVenueId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listMyVenuesUseCase = new ListMyVenuesUseCase(venueRepository, venueMembershipRepository);
    }

    @Test
    void returnsEmptyListWhenUserHasNoMemberships() {
        // Arrange
        when(venueMembershipRepository.findAllByUserId(userId)).thenReturn(List.of());

        // Act
        List<VenueMembershipResponse> response = listMyVenuesUseCase.execute(userId);

        // Assert
        assertThat(response).isEmpty();
        verify(venueMembershipRepository).findAllByUserId(userId);
        verifyNoInteractions(venueRepository);
    }

    @Test
    void returnsMembershipsSortedByVenueName() {
        // Arrange
        VenueMembershipEntity bistroMembership = VenueMembershipEntity.create(venueId, userId, VenueRole.WAITER);
        VenueMembershipEntity atelierMembership = VenueMembershipEntity.create(
                anotherVenueId, userId, VenueRole.MANAGER);
        VenueEntity bistro = VenueTestBuilder.aVenue()
                .withId(venueId)
                .withName("Milly Bistro")
                .withLocation("Barcelona, Spain")
                .build();
        VenueEntity atelier = VenueTestBuilder.aVenue()
                .withId(anotherVenueId)
                .withName("Atelier Milly")
                .withLocation("Paris, France")
                .build();
        when(venueMembershipRepository.findAllByUserId(userId))
                .thenReturn(List.of(bistroMembership, atelierMembership));
        when(venueRepository.findAllById(List.of(venueId, anotherVenueId))).thenReturn(List.of(bistro, atelier));

        // Act
        List<VenueMembershipResponse> response = listMyVenuesUseCase.execute(userId);

        // Assert
        assertThat(response).hasSize(2);
        assertThat(response.get(0).venueId()).isEqualTo(anotherVenueId);
        assertThat(response.get(0).venueName()).isEqualTo("Atelier Milly");
        assertThat(response.get(0).location()).isEqualTo("Paris, France");
        assertThat(response.get(0).role()).isEqualTo(VenueRole.MANAGER);
        assertThat(response.get(1).venueId()).isEqualTo(venueId);
        assertThat(response.get(1).venueName()).isEqualTo("Milly Bistro");
        assertThat(response.get(1).location()).isEqualTo("Barcelona, Spain");
        assertThat(response.get(1).role()).isEqualTo(VenueRole.WAITER);
        verify(venueMembershipRepository).findAllByUserId(userId);
        verify(venueRepository).findAllById(List.of(venueId, anotherVenueId));
    }

    @Test
    void propagatesFailureWhenMembershipVenueIsMissingFromRepositoryResult() {
        // Arrange
        VenueMembershipEntity membership = VenueMembershipEntity.create(venueId, userId, VenueRole.WAITER);
        when(venueMembershipRepository.findAllByUserId(userId)).thenReturn(List.of(membership));
        when(venueRepository.findAllById(anyIterable())).thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> listMyVenuesUseCase.execute(userId))
                .isInstanceOf(NullPointerException.class);

        verify(venueMembershipRepository).findAllByUserId(userId);
        verify(venueRepository).findAllById(List.of(venueId));
    }
}
