package com.milly.table.application.usecase;

import com.milly.common.exception.AccessDeniedException;
import com.milly.common.exception.ResourceNotFoundException;
import com.milly.table.application.dto.TableResponse;
import com.milly.table.application.dto.UpdateTableLabelRequest;
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
class UpdateTableLabelUseCaseTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    @Mock
    private TableJpaRepository tableRepository;

    private UpdateTableLabelUseCase updateTableLabelUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        updateTableLabelUseCase = new UpdateTableLabelUseCase(venueAuthorizationService, tableRepository);
    }

    @Test
    void updatesTableLabel() {
        // Arrange
        TableEntity table = aTable().withId(tableId).withVenueId(venueId).build();
        when(tableRepository.findByIdAndVenueId(tableId, venueId)).thenReturn(Optional.of(table));
        when(tableRepository.save(table)).thenReturn(table);

        // Act
        TableResponse response = updateTableLabelUseCase.execute(
                userId, venueId, tableId, new UpdateTableLabelRequest("Window 2"));

        // Assert
        assertThat(response.label()).isEqualTo("Window 2");
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
        verify(tableRepository).save(table);
    }

    @Test
    void updatesLabelOnInactiveTable() {
        // Arrange
        TableEntity inactiveTable = aTable().withId(tableId).withVenueId(venueId)
                .withStatus(TableStatus.INACTIVE).build();
        when(tableRepository.findByIdAndVenueId(tableId, venueId)).thenReturn(Optional.of(inactiveTable));
        when(tableRepository.save(inactiveTable)).thenReturn(inactiveTable);

        // Act
        TableResponse response = updateTableLabelUseCase.execute(
                userId, venueId, tableId, new UpdateTableLabelRequest("Archived 1"));

        // Assert
        assertThat(response.label()).isEqualTo("Archived 1");
        assertThat(response.status()).isEqualTo(TableStatus.INACTIVE);
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
    }

    @Test
    void throwsNotFoundWhenTableDoesNotBelongToVenue() {
        // Arrange
        when(tableRepository.findByIdAndVenueId(tableId, venueId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> updateTableLabelUseCase.execute(
                userId, venueId, tableId, new UpdateTableLabelRequest("Window 2")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(tableRepository, never()).save(any(TableEntity.class));
    }

    @Test
    void throwsAccessDeniedWhenUserIsNotManager() {
        // Arrange
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);

        // Act & Assert
        assertThatThrownBy(() -> updateTableLabelUseCase.execute(
                userId, venueId, tableId, new UpdateTableLabelRequest("Window 2")))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(tableRepository);
    }
}
