package com.milly.venue.application.usecase;

import com.milly.common.exception.AccessDeniedException;
import com.milly.venue.application.dto.CreateVenueRequest;
import com.milly.venue.application.dto.CreateVenueResponse;
import com.milly.venue.domain.entity.VenueEntity;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.domain.valueobject.VenueStatus;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateVenueUseCaseTest {

    @Mock
    private VenueJpaRepository venueRepository;

    @Mock
    private AssignVenueMembershipUseCase assignVenueMembershipUseCase;

    @Captor
    private ArgumentCaptor<VenueEntity> venueCaptor;

    private CreateVenueUseCase createVenueUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        createVenueUseCase = new CreateVenueUseCase(venueRepository, assignVenueMembershipUseCase);
    }

    @Test
    void createsActiveVenueAndManagerMembership() {
        // Arrange
        VenueMembershipEntity membership = VenueMembershipEntity.create(venueId, userId, VenueRole.MANAGER);
        when(venueRepository.save(any(VenueEntity.class))).thenAnswer(invocation -> {
            VenueEntity savedVenue = invocation.getArgument(0);
            savedVenue.setId(venueId);
            return savedVenue;
        });
        when(assignVenueMembershipUseCase.execute(venueId, userId, VenueRole.MANAGER)).thenReturn(membership);

        // Act
        CreateVenueResponse response = createVenueUseCase.execute(
                userId, new CreateVenueRequest("  Milly Bistro  ", "  Barcelona, Spain  "));

        // Assert
        assertThat(response.id()).isEqualTo(venueId);
        assertThat(response.name()).isEqualTo("Milly Bistro");
        assertThat(response.location()).isEqualTo("Barcelona, Spain");
        assertThat(response.role()).isEqualTo(VenueRole.MANAGER);
        verify(venueRepository).save(venueCaptor.capture());
        assertThat(venueCaptor.getValue().getName()).isEqualTo("Milly Bistro");
        assertThat(venueCaptor.getValue().getLocation()).isEqualTo("Barcelona, Spain");
        assertThat(venueCaptor.getValue().getStatus()).isEqualTo(VenueStatus.ACTIVE);
        verify(assignVenueMembershipUseCase).execute(venueId, userId, VenueRole.MANAGER);
    }

    @Test
    void propagatesMembershipAssignmentFailure() {
        // Arrange
        when(venueRepository.save(any(VenueEntity.class))).thenAnswer(invocation -> {
            VenueEntity savedVenue = invocation.getArgument(0);
            savedVenue.setId(venueId);
            return savedVenue;
        });
        when(assignVenueMembershipUseCase.execute(venueId, userId, VenueRole.MANAGER))
                .thenThrow(new AccessDeniedException());

        // Act & Assert
        assertThatThrownBy(() -> createVenueUseCase.execute(
                userId, new CreateVenueRequest("Milly Bistro", "Barcelona, Spain")))
                .isInstanceOf(AccessDeniedException.class);

        verify(venueRepository).save(any(VenueEntity.class));
        verify(assignVenueMembershipUseCase).execute(venueId, userId, VenueRole.MANAGER);
    }
}