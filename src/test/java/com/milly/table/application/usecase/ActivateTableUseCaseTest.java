package com.milly.table.application.usecase;

import com.milly.common.application.exception.AccessDeniedException;
import com.milly.common.application.exception.ResourceNotFoundException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivateTableUseCaseTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    @Mock
    private TableJpaRepository tableRepository;

    private ActivateTableUseCase activateTableUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        activateTableUseCase = new ActivateTableUseCase(venueAuthorizationService, tableRepository);
    }

    @Test
    void activatesInactiveTable() {
        // Arrange
        TableEntity table = aTable().withId(tableId).withVenueId(venueId)
                .withStatus(TableStatus.INACTIVE).build();
        when(tableRepository.findByIdAndVenueId(tableId, venueId)).thenReturn(Optional.of(table));

        // Act
        activateTableUseCase.execute(userId, venueId, tableId);

        // Assert
        assertThat(table.getStatus()).isEqualTo(TableStatus.ACTIVE);
        verify(venueAuthorizationService).requireAtLeastRole(userId, venueId, VenueRole.MANAGER);
        verify(tableRepository).save(table);
    }

    @Test
    void activatesAlreadyActiveTableWithoutError() {
        // Arrange
        TableEntity activeTable = aTable().withId(tableId).withVenueId(venueId).build();
        when(tableRepository.findByIdAndVenueId(tableId, venueId)).thenReturn(Optional.of(activeTable));

        // Act
        activateTableUseCase.execute(userId, venueId, tableId);

        // Assert
        assertThat(activeTable.getStatus()).isEqualTo(TableStatus.ACTIVE);
        verify(venueAuthorizationService).requireAtLeastRole(userId, venueId, VenueRole.MANAGER);
        verify(tableRepository).save(activeTable);
    }

    @Test
    void throwsNotFoundWhenTableDoesNotBelongToVenue() {
        // Arrange
        when(tableRepository.findByIdAndVenueId(tableId, venueId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> activateTableUseCase.execute(userId, venueId, tableId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(tableRepository, never()).save(any(TableEntity.class));
    }

    @Test
    void throwsAccessDeniedWhenUserIsNotManager() {
        // Arrange
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireAtLeastRole(userId, venueId, VenueRole.MANAGER);

        // Act & Assert
        assertThatThrownBy(() -> activateTableUseCase.execute(userId, venueId, tableId))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(tableRepository);
    }
}
