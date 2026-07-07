package com.milly.venue.application.usecase;

import com.milly.common.exception.AccessDeniedException;
import com.milly.common.exception.ResourceNotFoundException;
import com.milly.venue.application.dto.CreateVenueInvitationRequest;
import com.milly.venue.application.dto.CreateVenueInvitationResponse;
import com.milly.venue.application.port.outbound.VenueInvitationStore;
import com.milly.venue.application.service.VenueAuthorizationService;
import com.milly.venue.application.service.VenueInvitationUrlBuilder;
import com.milly.venue.application.usecase.builder.VenueTestBuilder;
import com.milly.venue.domain.model.VenueInvitation;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateVenueInvitationUseCaseTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    @Mock
    private VenueJpaRepository venueRepository;

    @Mock
    private VenueInvitationStore venueInvitationStore;

    @Mock
    private VenueInvitationUrlBuilder venueInvitationUrlBuilder;

    private CreateVenueInvitationUseCase createVenueInvitationUseCase;

    private final UUID managerId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        createVenueInvitationUseCase = new CreateVenueInvitationUseCase(
                venueAuthorizationService,
                venueRepository,
                venueInvitationStore,
                venueInvitationUrlBuilder);
    }

    @ParameterizedTest
    @EnumSource(VenueRole.class)
    void createsInvitationWithSelectedRole(VenueRole role) {
        // Arrange
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(VenueTestBuilder.aVenue().withId(venueId).build()));
        when(venueInvitationUrlBuilder.build(any(UUID.class))).thenAnswer(invocation ->
                "http://localhost:3000/join-venue/invite/" + invocation.getArgument(0));

        // Act
        CreateVenueInvitationResponse response = createVenueInvitationUseCase.execute(
                managerId, venueId, new CreateVenueInvitationRequest(role));

        // Assert
        assertThat(response.token()).isNotNull();
        assertThat(response.role()).isEqualTo(role);
        assertThat(response.inviteUrl()).isEqualTo("http://localhost:3000/join-venue/invite/" + response.token());
        verify(venueAuthorizationService).requireRole(managerId, venueId, VenueRole.MANAGER);
        ArgumentCaptor<VenueInvitation> captor = ArgumentCaptor.forClass(VenueInvitation.class);
        verify(venueInvitationStore).register(captor.capture());
        assertThat(captor.getValue().token()).isEqualTo(response.token());
        assertThat(captor.getValue().venueId()).isEqualTo(venueId);
        assertThat(captor.getValue().role()).isEqualTo(role);
        assertThat(captor.getValue().createdByUserId()).isEqualTo(managerId);
    }

    @Test
    void throwsAccessDeniedWhenUserIsNotManager() {
        // Arrange
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireRole(managerId, venueId, VenueRole.MANAGER);

        // Act & Assert
        assertThatThrownBy(() -> createVenueInvitationUseCase.execute(
                managerId, venueId, new CreateVenueInvitationRequest(VenueRole.WAITER)))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(venueRepository, venueInvitationStore, venueInvitationUrlBuilder);
    }

    @Test
    void throwsNotFoundWhenVenueDoesNotExist() {
        // Arrange
        when(venueRepository.findById(venueId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> createVenueInvitationUseCase.execute(
                managerId, venueId, new CreateVenueInvitationRequest(VenueRole.WAITER)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(venueInvitationStore, venueInvitationUrlBuilder);
    }
}
