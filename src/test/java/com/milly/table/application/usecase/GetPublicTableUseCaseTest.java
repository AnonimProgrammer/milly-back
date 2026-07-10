package com.milly.table.application.usecase;

import com.milly.common.exception.ResourceNotFoundException;
import com.milly.table.application.dto.PublicTableResponse;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPublicTableUseCaseTest {

    @Mock
    private TableJpaRepository tableRepository;

    private GetPublicTableUseCase getPublicTableUseCase;

    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        getPublicTableUseCase = new GetPublicTableUseCase(tableRepository);
    }

    @Test
    void returnsActiveTable() {
        // Arrange
        TableEntity activeTable = aTable().withId(tableId).withVenueId(venueId).build();
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(activeTable));

        // Act
        PublicTableResponse response = getPublicTableUseCase.execute(tableId);

        // Assert
        assertThat(response.id()).isEqualTo(tableId);
        assertThat(response.venueId()).isEqualTo(venueId);
        assertThat(response.status()).isEqualTo(TableStatus.ACTIVE);
    }

    @Test
    void throwsNotFoundWhenTableIsMissing() {
        // Arrange
        when(tableRepository.findById(tableId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> getPublicTableUseCase.execute(tableId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenTableIsInactive() {
        // Arrange
        TableEntity inactiveTable = aTable().withId(tableId).withVenueId(venueId)
                .withStatus(TableStatus.INACTIVE).build();
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(inactiveTable));

        // Act & Assert
        assertThatThrownBy(() -> getPublicTableUseCase.execute(tableId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}