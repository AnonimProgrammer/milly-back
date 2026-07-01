package com.milly.venue.application.usecase;

import com.milly.venue.domain.entity.VenueEntity;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.domain.valueobject.VenueStatus;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueJpaRepository;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateVenueUseCaseTests {

    @Mock
    private VenueJpaRepository venueRepository;

    @Mock
    private VenueMembershipJpaRepository venueMembershipRepository;

    @InjectMocks
    private CreateVenueUseCase createVenueUseCase;

    @Test
    void executeCreatesActiveVenueAndManagerMembership() {
        UUID userId = UUID.randomUUID();
        when(venueRepository.save(any(VenueEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(venueMembershipRepository.save(any(VenueMembershipEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VenueEntity result = createVenueUseCase.execute(
                userId, "  Milly Bistro  ", "  Barcelona, Spain  ");

        assertThat(result.getName()).isEqualTo("Milly Bistro");
        assertThat(result.getLocation()).isEqualTo("Barcelona, Spain");
        assertThat(result.getStatus()).isEqualTo(VenueStatus.ACTIVE);

        ArgumentCaptor<VenueMembershipEntity> membershipCaptor =
                ArgumentCaptor.forClass(VenueMembershipEntity.class);
        verify(venueMembershipRepository).save(membershipCaptor.capture());
        VenueMembershipEntity membership = membershipCaptor.getValue();
        assertThat(membership.getVenueId()).isEqualTo(result.getId());
        assertThat(membership.getUserId()).isEqualTo(userId);
        assertThat(membership.getRole()).isEqualTo(VenueRole.MANAGER);
    }

    @Test
    void executePropagatesMembershipFailureForTransactionRollback() {
        UUID userId = UUID.randomUUID();
        RuntimeException persistenceFailure = new RuntimeException("membership save failed");
        when(venueRepository.save(any(VenueEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(venueMembershipRepository.save(any(VenueMembershipEntity.class)))
                .thenThrow(persistenceFailure);

        assertThatThrownBy(() -> createVenueUseCase.execute(
                userId, "Milly Bistro", "Barcelona, Spain"))
                .isSameAs(persistenceFailure);

        verify(venueRepository).save(any(VenueEntity.class));
        verify(venueMembershipRepository).save(any(VenueMembershipEntity.class));
    }
}
