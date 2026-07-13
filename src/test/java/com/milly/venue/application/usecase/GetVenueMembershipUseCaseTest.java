package com.milly.venue.application.usecase;

import com.milly.common.application.exception.AccessDeniedException;
import com.milly.common.application.exception.InactiveMembershipException;
import com.milly.common.application.exception.ResourceNotFoundException;
import com.milly.venue.application.dto.VenueMembershipResponse;
import com.milly.venue.application.service.VenueAuthorizationService;
import com.milly.venue.application.usecase.builder.VenueTestBuilder;
import com.milly.venue.domain.entity.VenueEntity;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetVenueMembershipUseCaseTest {

    @Mock
    private VenueJpaRepository venueRepository;

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    private GetVenueMembershipUseCase getVenueMembershipUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        getVenueMembershipUseCase = new GetVenueMembershipUseCase(venueRepository, venueAuthorizationService);
    }

    @Test
    void returnsMembershipWhenVenueExistsAndUserIsActiveMember() {
        // Arrange
        VenueEntity venue = VenueTestBuilder.aVenue()
                .withId(venueId)
                .withName("Milly Bistro")
                .withLocation("Barcelona, Spain")
                .build();
        VenueMembershipEntity membership = VenueMembershipEntity.create(venueId, userId, VenueRole.MANAGER);
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(venueAuthorizationService.requireActiveMember(userId, venueId)).thenReturn(membership);

        // Act
        VenueMembershipResponse response = getVenueMembershipUseCase.execute(venueId, userId);

        // Assert
        assertThat(response.venueId()).isEqualTo(venueId);
        assertThat(response.venueName()).isEqualTo("Milly Bistro");
        assertThat(response.location()).isEqualTo("Barcelona, Spain");
        assertThat(response.role()).isEqualTo(VenueRole.MANAGER);
        verify(venueRepository).findById(venueId);
        verify(venueAuthorizationService).requireActiveMember(userId, venueId);
    }

    @Test
    void throwsNotFoundWhenVenueDoesNotExist() {
        // Arrange
        when(venueRepository.findById(venueId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> getVenueMembershipUseCase.execute(venueId, userId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(venueRepository).findById(venueId);
        verifyNoInteractions(venueAuthorizationService);
    }

    @Test
    void throwsAccessDeniedWhenUserIsNotMember() {
        // Arrange
        VenueEntity venue = VenueTestBuilder.aVenue().withId(venueId).build();
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(venueAuthorizationService.requireActiveMember(userId, venueId))
                .thenThrow(new AccessDeniedException());

        // Act & Assert
        assertThatThrownBy(() -> getVenueMembershipUseCase.execute(venueId, userId))
                .isInstanceOf(AccessDeniedException.class);

        verify(venueRepository).findById(venueId);
        verify(venueAuthorizationService).requireActiveMember(userId, venueId);
    }

    @Test
    void throwsInactiveMembershipWhenUserIsBlocked() {
        // Arrange
        VenueEntity venue = VenueTestBuilder.aVenue().withId(venueId).build();
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(venueAuthorizationService.requireActiveMember(userId, venueId))
                .thenThrow(new InactiveMembershipException());

        // Act & Assert
        assertThatThrownBy(() -> getVenueMembershipUseCase.execute(venueId, userId))
                .isInstanceOf(InactiveMembershipException.class);

        verify(venueRepository).findById(venueId);
        verify(venueAuthorizationService).requireActiveMember(userId, venueId);
    }
}
