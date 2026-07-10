package com.milly.venue.application.usecase;

import com.milly.common.exception.InvalidInvitationException;
import com.milly.common.exception.ResourceNotFoundException;
import com.milly.common.exception.VenueMembershipAlreadyExistsException;
import com.milly.venue.application.dto.RedeemVenueInvitationRequest;
import com.milly.venue.application.dto.VenueMembershipResponse;
import com.milly.venue.application.port.outbound.VenueInvitationStore;
import com.milly.venue.application.usecase.builder.VenueInvitationTestBuilder;
import com.milly.venue.application.usecase.builder.VenueTestBuilder;
import com.milly.venue.domain.entity.VenueEntity;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.model.VenueInvitation;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueJpaRepository;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedeemVenueInvitationUseCaseTest {

    @Mock
    private VenueInvitationStore venueInvitationStore;

    @Mock
    private VenueMembershipJpaRepository venueMembershipRepository;

    @Mock
    private VenueJpaRepository venueRepository;

    @Mock
    private AssignVenueMembershipUseCase assignVenueMembershipUseCase;

    private RedeemVenueInvitationUseCase redeemVenueInvitationUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID token = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        redeemVenueInvitationUseCase = new RedeemVenueInvitationUseCase(
                venueInvitationStore,
                venueMembershipRepository,
                venueRepository,
                assignVenueMembershipUseCase);
    }

    @ParameterizedTest
    @EnumSource(VenueRole.class)
    void createsMembershipWhenTokenIsValid(VenueRole role) {
        // Arrange
        VenueInvitation invitation = aStoredInvitation(role);
        VenueEntity venue = VenueTestBuilder.aVenue().withId(venueId).build();
        VenueMembershipEntity membership = VenueMembershipEntity.create(venueId, userId, role);
        when(venueInvitationStore.find(token)).thenReturn(Optional.of(invitation));
        when(venueMembershipRepository.findByUserIdAndVenueId(userId, venueId)).thenReturn(Optional.empty());
        when(venueInvitationStore.claim(token)).thenReturn(Optional.of(invitation));
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(assignVenueMembershipUseCase.execute(venueId, userId, role)).thenReturn(membership);

        // Act
        VenueMembershipResponse response = redeemVenueInvitationUseCase.execute(
                userId, new RedeemVenueInvitationRequest(token));

        // Assert
        assertThat(response.venueId()).isEqualTo(venueId);
        assertThat(response.venueName()).isEqualTo(venue.getName());
        assertThat(response.location()).isEqualTo(venue.getLocation());
        assertThat(response.role()).isEqualTo(role);
        verify(venueInvitationStore).claim(token);
        verify(assignVenueMembershipUseCase).execute(venueId, userId, role);
    }

    @Test
    void throwsInvalidInvitationWhenTokenIsMissing() {
        // Arrange
        when(venueInvitationStore.find(token)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> redeemVenueInvitationUseCase.execute(
                userId, new RedeemVenueInvitationRequest(token)))
                .isInstanceOf(InvalidInvitationException.class);

        verifyNoInteractions(venueMembershipRepository, venueRepository, assignVenueMembershipUseCase);
        verifyNoMoreInteractions(venueInvitationStore);
    }

    @Test
    void throwsAlreadyMemberWithoutConsumingToken() {
        // Arrange
        VenueInvitation invitation = aStoredInvitation(VenueRole.WAITER);
        when(venueInvitationStore.find(token)).thenReturn(Optional.of(invitation));
        when(venueMembershipRepository.findByUserIdAndVenueId(userId, venueId))
                .thenReturn(Optional.of(VenueMembershipEntity.create(venueId, userId, VenueRole.WAITER)));

        // Act & Assert
        assertThatThrownBy(() -> redeemVenueInvitationUseCase.execute(
                userId, new RedeemVenueInvitationRequest(token)))
                .isInstanceOf(VenueMembershipAlreadyExistsException.class);

        verifyNoInteractions(venueRepository, assignVenueMembershipUseCase);
        verifyNoMoreInteractions(venueInvitationStore);
    }

    @Test
    void throwsInvalidInvitationWhenTokenWasAlreadyUsed() {
        // Arrange
        VenueInvitation invitation = aStoredInvitation(VenueRole.WAITER);
        when(venueInvitationStore.find(token)).thenReturn(Optional.of(invitation));
        when(venueMembershipRepository.findByUserIdAndVenueId(userId, venueId)).thenReturn(Optional.empty());
        when(venueInvitationStore.claim(token)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> redeemVenueInvitationUseCase.execute(
                userId, new RedeemVenueInvitationRequest(token)))
                .isInstanceOf(InvalidInvitationException.class);

        verifyNoInteractions(venueRepository, assignVenueMembershipUseCase);
    }

    @Test
    void throwsNotFoundWhenVenueWasDeleted() {
        // Arrange
        VenueInvitation invitation = aStoredInvitation(VenueRole.MANAGER);
        when(venueInvitationStore.find(token)).thenReturn(Optional.of(invitation));
        when(venueMembershipRepository.findByUserIdAndVenueId(userId, venueId)).thenReturn(Optional.empty());
        when(venueInvitationStore.claim(token)).thenReturn(Optional.of(invitation));
        when(venueRepository.findById(venueId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> redeemVenueInvitationUseCase.execute(
                userId, new RedeemVenueInvitationRequest(token)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(assignVenueMembershipUseCase);
    }

    private VenueInvitation aStoredInvitation(VenueRole role) {
        return VenueInvitationTestBuilder.aVenueInvitation()
                .withToken(token)
                .withVenueId(venueId)
                .withRole(role)
                .build();
    }
}