package com.milly.table.application.usecase;

import com.milly.common.exception.AccessDeniedException;
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

import java.util.List;
import java.util.UUID;

import static com.milly.table.application.usecase.builder.TableTestBuilder.aTable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListTablesUseCaseTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    @Mock
    private TableJpaRepository tableRepository;

    private ListTablesUseCase listTablesUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listTablesUseCase = new ListTablesUseCase(venueAuthorizationService, tableRepository);
    }

    @Test
    void returnsAllVenueTablesIncludingInactiveOnes() {
        // Arrange
        TableEntity activeTable = aTable().withVenueId(venueId).build();
        TableEntity inactiveTable = aTable().withVenueId(venueId).withStatus(TableStatus.INACTIVE).build();
        when(tableRepository.findByVenueIdOrderByLabelAsc(venueId)).thenReturn(List.of(activeTable, inactiveTable));

        // Act
        List<TableResponse> response = listTablesUseCase.execute(userId, venueId);

        // Assert
        assertThat(response).hasSize(2);
        assertThat(response).extracting(TableResponse::status)
                .containsExactly(TableStatus.ACTIVE, TableStatus.INACTIVE);
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
    }

    @Test
    void returnsEmptyListWhenVenueHasNoTables() {
        // Arrange
        when(tableRepository.findByVenueIdOrderByLabelAsc(venueId)).thenReturn(List.of());

        // Act
        List<TableResponse> response = listTablesUseCase.execute(userId, venueId);

        // Assert
        assertThat(response).isEmpty();
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
    }

    @Test
    void throwsAccessDeniedWhenUserIsNotManager() {
        // Arrange
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);

        // Act & Assert
        assertThatThrownBy(() -> listTablesUseCase.execute(userId, venueId))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(tableRepository);
    }
}
