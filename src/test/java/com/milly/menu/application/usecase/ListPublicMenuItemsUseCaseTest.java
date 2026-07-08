package com.milly.menu.application.usecase;

import com.milly.common.domain.valueobject.Money;
import com.milly.common.exception.ResourceNotFoundException;
import com.milly.menu.application.dto.MenuItemResponse;
import com.milly.menu.domain.entity.MenuItemEntity;
import com.milly.menu.domain.valueobject.MenuItemStatus;
import com.milly.menu.infrastructure.adapter.outbound.persistence.MenuItemJpaRepository;
import com.milly.table.application.usecase.builder.TableTestBuilder;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListPublicMenuItemsUseCaseTest {

    @Mock
    private TableJpaRepository tableRepository;

    @Mock
    private MenuItemJpaRepository menuItemRepository;

    private ListPublicMenuItemsUseCase listPublicMenuItemsUseCase;

    private final UUID tableId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();
    private final UUID anotherItemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listPublicMenuItemsUseCase = new ListPublicMenuItemsUseCase(tableRepository, menuItemRepository);
    }

    @Test
    void returnsActiveMenuItemsForActiveTableVenue() {
        // Arrange
        TableEntity table = activeTable();
        MenuItemEntity pasta = menuItem(anotherItemId, "Pasta", "Tomato");
        MenuItemEntity pizza = menuItem(itemId, "Pizza", "Cheese");
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(table));
        when(menuItemRepository.findByVenueIdAndStatusOrderByNameAsc(venueId, MenuItemStatus.ACTIVE))
                .thenReturn(List.of(pasta, pizza));

        // Act
        List<MenuItemResponse> response = listPublicMenuItemsUseCase.execute(tableId);

        // Assert
        assertThat(response).hasSize(2);
        assertThat(response).extracting(MenuItemResponse::id).containsExactly(anotherItemId, itemId);
        assertThat(response).extracting(MenuItemResponse::venueId).containsExactly(venueId, venueId);
        assertThat(response).extracting(MenuItemResponse::name).containsExactly("Pasta", "Pizza");
        assertThat(response).extracting(MenuItemResponse::status)
                .containsExactly(MenuItemStatus.ACTIVE, MenuItemStatus.ACTIVE);
        verify(tableRepository).findById(tableId);
        verify(menuItemRepository).findByVenueIdAndStatusOrderByNameAsc(venueId, MenuItemStatus.ACTIVE);
    }

    @Test
    void returnsEmptyListWhenActiveTableVenueHasNoActiveItems() {
        // Arrange
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(activeTable()));
        when(menuItemRepository.findByVenueIdAndStatusOrderByNameAsc(venueId, MenuItemStatus.ACTIVE))
                .thenReturn(List.of());

        // Act
        List<MenuItemResponse> response = listPublicMenuItemsUseCase.execute(tableId);

        // Assert
        assertThat(response).isEmpty();
        verify(tableRepository).findById(tableId);
        verify(menuItemRepository).findByVenueIdAndStatusOrderByNameAsc(venueId, MenuItemStatus.ACTIVE);
    }

    @Test
    void throwsNotFoundWhenTableDoesNotExist() {
        // Arrange
        when(tableRepository.findById(tableId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> listPublicMenuItemsUseCase.execute(tableId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(tableRepository).findById(tableId);
        verifyNoInteractions(menuItemRepository);
    }

    @Test
    void throwsNotFoundWhenTableIsInactive() {
        // Arrange
        TableEntity table = TableTestBuilder.aTable()
                .withId(tableId)
                .withVenueId(venueId)
                .withStatus(TableStatus.INACTIVE)
                .build();
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(table));

        // Act & Assert
        assertThatThrownBy(() -> listPublicMenuItemsUseCase.execute(tableId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(tableRepository).findById(tableId);
        verifyNoInteractions(menuItemRepository);
    }

    private TableEntity activeTable() {
        return TableTestBuilder.aTable()
                .withId(tableId)
                .withVenueId(venueId)
                .withStatus(TableStatus.ACTIVE)
                .build();
    }

    private MenuItemEntity menuItem(UUID id, String name, String description) {
        MenuItemEntity item = MenuItemEntity.create(
                venueId, name, description, Money.of("12.50"), MenuItemStatus.ACTIVE);
        item.setId(id);
        return item;
    }
}
