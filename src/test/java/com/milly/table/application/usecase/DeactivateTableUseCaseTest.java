package com.milly.table.application.usecase;

import com.milly.common.exception.AccessDeniedException;
import com.milly.common.exception.ResourceNotFoundException;
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
class DeactivateTableUseCaseTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    @Mock
    private TableJpaRepository tableRepository;

    private DeactivateTableUseCase deactivateTableUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        deactivateTableUseCase = new DeactivateTableUseCase(venueAuthorizationService, tableRepository);
    }

    @Test
    void deactivatesActiveTable() {
        TableEntity table = aTable().withId(tableId).withVenueId(venueId).build();
        when(tableRepository.findByIdAndVenueId(tableId, venueId)).thenReturn(Optional.of(table));

        deactivateTableUseCase.execute(userId, venueId, tableId);

        assertThat(table.getStatus()).isEqualTo(TableStatus.INACTIVE);
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
        verify(tableRepository).save(table);
    }

    @Test
    void deactivatesAlreadyInactiveTableWithoutError() {
        TableEntity inactiveTable = aTable().withId(tableId).withVenueId(venueId)
                .withStatus(TableStatus.INACTIVE).build();
        when(tableRepository.findByIdAndVenueId(tableId, venueId)).thenReturn(Optional.of(inactiveTable));

        deactivateTableUseCase.execute(userId, venueId, tableId);

        assertThat(inactiveTable.getStatus()).isEqualTo(TableStatus.INACTIVE);
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
        verify(tableRepository).save(inactiveTable);
    }

    @Test
    void deactivatesTableThatHasExistingQrImageUrl() {
        String existingQrImageUrl = "https://storage.local/venues/%s/tables/%s/qr.png".formatted(venueId, tableId);
        TableEntity tableWithQr = aTable().withId(tableId).withVenueId(venueId).build();
        tableWithQr.setQrImageUrl(existingQrImageUrl);
        when(tableRepository.findByIdAndVenueId(tableId, venueId)).thenReturn(Optional.of(tableWithQr));

        deactivateTableUseCase.execute(userId, venueId, tableId);

        assertThat(tableWithQr.getStatus()).isEqualTo(TableStatus.INACTIVE);
        assertThat(tableWithQr.getQrImageUrl()).isEqualTo(existingQrImageUrl);
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
        verify(tableRepository).save(tableWithQr);
    }

    @Test
    void throwsNotFoundWhenTableDoesNotBelongToVenue() {
        when(tableRepository.findByIdAndVenueId(tableId, venueId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deactivateTableUseCase.execute(userId, venueId, tableId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(tableRepository, never()).save(any(TableEntity.class));
    }

    @Test
    void throwsAccessDeniedWhenUserIsNotManager() {
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);

        assertThatThrownBy(() -> deactivateTableUseCase.execute(userId, venueId, tableId))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(tableRepository);
    }
}
