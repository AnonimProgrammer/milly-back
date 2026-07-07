package com.milly.table.application.usecase;

import com.milly.common.exception.AccessDeniedException;
import com.milly.common.exception.ResourceNotFoundException;
import com.milly.table.application.dto.TableResponse;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import com.milly.venue.application.service.VenueAuthorizationService;
import com.milly.venue.domain.valueobject.VenueRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.milly.table.application.usecase.builder.TableTestBuilder.aTable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTableUseCaseTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    @Mock
    private TableJpaRepository tableRepository;

    private GetTableUseCase getTableUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        getTableUseCase = new GetTableUseCase(venueAuthorizationService, tableRepository);
    }

    @Test
    void returnsTableForVenue() {
        // Arrange
        TableEntity table = aTable().withId(tableId).withVenueId(venueId).build();
        when(tableRepository.findByIdAndVenueId(tableId, venueId)).thenReturn(Optional.of(table));

        // Act
        TableResponse response = getTableUseCase.execute(userId, venueId, tableId);

        // Assert
        assertThat(response.id()).isEqualTo(tableId);
        assertThat(response.venueId()).isEqualTo(venueId);
        assertThat(response.status()).isEqualTo(TableStatus.ACTIVE);
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
    }

    @Test
    void returnsInactiveTableForManager() {
        // Arrange
        TableEntity inactiveTable = aTable().withId(tableId).withVenueId(venueId)
                .withStatus(TableStatus.INACTIVE).build();
        when(tableRepository.findByIdAndVenueId(tableId, venueId)).thenReturn(Optional.of(inactiveTable));

        // Act
        TableResponse response = getTableUseCase.execute(userId, venueId, tableId);

        // Assert
        assertThat(response.status()).isEqualTo(TableStatus.INACTIVE);
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
    }

    @Test
    void throwsNotFoundWhenTableDoesNotBelongToVenue() {
        // Arrange
        when(tableRepository.findByIdAndVenueId(tableId, venueId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> getTableUseCase.execute(userId, venueId, tableId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void throwsAccessDeniedWhenUserIsNotManager() {
        // Arrange
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);

        // Act & Assert
        assertThatThrownBy(() -> getTableUseCase.execute(userId, venueId, tableId))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(tableRepository);
    }
}
