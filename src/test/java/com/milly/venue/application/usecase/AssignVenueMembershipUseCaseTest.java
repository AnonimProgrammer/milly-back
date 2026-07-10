package com.milly.venue.application.usecase;

import com.milly.common.exception.VenueMembershipAlreadyExistsException;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
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
class AssignVenueMembershipUseCaseTest {

    @Mock
    private VenueMembershipJpaRepository venueMembershipRepository;

    @Captor
    private ArgumentCaptor<VenueMembershipEntity> membershipCaptor;

    private AssignVenueMembershipUseCase assignVenueMembershipUseCase;

    private final UUID venueId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID membershipId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assignVenueMembershipUseCase = new AssignVenueMembershipUseCase(venueMembershipRepository);
    }

    @Test
    void persistsMembershipWithSelectedRole() {
        // Arrange
        when(venueMembershipRepository.save(any(VenueMembershipEntity.class))).thenAnswer(invocation -> {
            VenueMembershipEntity savedMembership = invocation.getArgument(0);
            savedMembership.setId(membershipId);
            return savedMembership;
        });

        // Act
        VenueMembershipEntity membership = assignVenueMembershipUseCase.execute(venueId, userId, VenueRole.WAITER);

        // Assert
        assertThat(membership.getId()).isEqualTo(membershipId);
        assertThat(membership.getVenueId()).isEqualTo(venueId);
        assertThat(membership.getUserId()).isEqualTo(userId);
        assertThat(membership.getRole()).isEqualTo(VenueRole.WAITER);
        verify(venueMembershipRepository).save(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue().getVenueId()).isEqualTo(venueId);
        assertThat(membershipCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(membershipCaptor.getValue().getRole()).isEqualTo(VenueRole.WAITER);
    }

    @Test
    void propagatesRepositoryFailure() {
        // Arrange
        when(venueMembershipRepository.save(any(VenueMembershipEntity.class)))
                .thenThrow(new VenueMembershipAlreadyExistsException());

        // Act & Assert
        assertThatThrownBy(() -> assignVenueMembershipUseCase.execute(venueId, userId, VenueRole.MANAGER))
                .isInstanceOf(VenueMembershipAlreadyExistsException.class);

        verify(venueMembershipRepository).save(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue().getVenueId()).isEqualTo(venueId);
        assertThat(membershipCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(membershipCaptor.getValue().getRole()).isEqualTo(VenueRole.MANAGER);
    }
}