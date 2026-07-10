package com.milly.table.application.usecase;

import com.milly.common.exception.AccessDeniedException;
import com.milly.table.application.dto.CreateTableRequest;
import com.milly.table.application.dto.TableResponse;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import com.milly.venue.application.service.VenueAuthorizationService;
import com.milly.venue.domain.valueobject.VenueRole;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTableUseCaseTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    @Mock
    private TableJpaRepository tableRepository;

    @Captor
    private ArgumentCaptor<TableEntity> tableCaptor;

    private CreateTableUseCase createTableUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        createTableUseCase = new CreateTableUseCase(venueAuthorizationService, tableRepository);
    }

    @Test
    void persistsActiveTableForVenue() {
        // Arrange
        when(tableRepository.save(any(TableEntity.class))).thenAnswer(invocation -> {
            TableEntity savedTable = invocation.getArgument(0);
            savedTable.setId(tableId);
            return savedTable;
        });

        // Act
        TableResponse response = createTableUseCase.execute(userId, venueId, new CreateTableRequest("Patio 1"));

        // Assert
        assertThat(response.id()).isEqualTo(tableId);
        assertThat(response.venueId()).isEqualTo(venueId);
        assertThat(response.label()).isEqualTo("Patio 1");
        assertThat(response.status()).isEqualTo(TableStatus.ACTIVE);
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
        verify(tableRepository).save(tableCaptor.capture());
        assertThat(tableCaptor.getValue().getVenueId()).isEqualTo(venueId);
        assertThat(tableCaptor.getValue().getLabel()).isEqualTo("Patio 1");
        assertThat(tableCaptor.getValue().getStatus()).isEqualTo(TableStatus.ACTIVE);
    }

    @Test
    void throwsAccessDeniedWhenUserIsNotManager() {
        // Arrange
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);

        // Act & Assert
        assertThatThrownBy(() -> createTableUseCase.execute(userId, venueId, new CreateTableRequest("Patio 1")))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(tableRepository);
    }
}