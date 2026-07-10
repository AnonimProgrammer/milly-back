package com.milly.menu.application.usecase;

import com.milly.common.domain.valueobject.Money;
import com.milly.common.exception.AccessDeniedException;
import com.milly.common.exception.ResourceNotFoundException;
import com.milly.menu.application.dto.MenuItemResponse;
import com.milly.menu.domain.entity.MenuItemEntity;
import com.milly.menu.domain.valueobject.MenuItemStatus;
import com.milly.menu.infrastructure.adapter.outbound.persistence.MenuItemJpaRepository;
import com.milly.venue.application.service.VenueAuthorizationService;
import com.milly.venue.domain.valueobject.VenueRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuItemQueryUseCasesTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    @Mock
    private MenuItemJpaRepository menuItemRepository;

    private ListMenuItemsUseCase listMenuItemsUseCase;
    private GetMenuItemUseCase getMenuItemUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();
    private final UUID anotherItemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listMenuItemsUseCase = new ListMenuItemsUseCase(venueAuthorizationService, menuItemRepository);
        getMenuItemUseCase = new GetMenuItemUseCase(venueAuthorizationService, menuItemRepository);
    }

    @Test
    void listReturnsOnlyRepositoryActiveItems() {
        // Arrange
        MenuItemEntity item = menuItem(itemId, "Pizza", "Cheese");
        MenuItemEntity anotherItem = menuItem(anotherItemId, "Pasta", "Tomato");
        when(menuItemRepository.findByVenueIdAndStatusOrderByNameAsc(venueId, MenuItemStatus.ACTIVE))
                .thenReturn(List.of(anotherItem, item));

        // Act
        List<MenuItemResponse> response = listMenuItemsUseCase.execute(userId, venueId);

        // Assert
        assertThat(response).hasSize(2);
        assertThat(response).extracting(MenuItemResponse::id).containsExactly(anotherItemId, itemId);
        assertThat(response).extracting(MenuItemResponse::name).containsExactly("Pasta", "Pizza");
        assertThat(response).extracting(MenuItemResponse::status)
                .containsExactly(MenuItemStatus.ACTIVE, MenuItemStatus.ACTIVE);
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
        verify(menuItemRepository).findByVenueIdAndStatusOrderByNameAsc(venueId, MenuItemStatus.ACTIVE);
    }

    @Test
    void listReturnsEmptyListWhenVenueHasNoActiveItems() {
        // Arrange
        when(menuItemRepository.findByVenueIdAndStatusOrderByNameAsc(venueId, MenuItemStatus.ACTIVE))
                .thenReturn(List.of());

        // Act
        List<MenuItemResponse> response = listMenuItemsUseCase.execute(userId, venueId);

        // Assert
        assertThat(response).isEmpty();
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
        verify(menuItemRepository).findByVenueIdAndStatusOrderByNameAsc(venueId, MenuItemStatus.ACTIVE);
    }

    @Test
    void listStopsWhenManagerAuthorizationFails() {
        // Arrange
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> listMenuItemsUseCase.execute(userId, venueId));

        verifyNoInteractions(menuItemRepository);
    }

    @Test
    void getReturnsActiveItemFromRequestedVenue() {
        // Arrange
        MenuItemEntity item = menuItem(itemId, "Pizza", "Cheese");
        when(menuItemRepository.findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE))
                .thenReturn(Optional.of(item));

        // Act
        MenuItemResponse response = getMenuItemUseCase.execute(userId, venueId, itemId);

        // Assert
        assertThat(response.id()).isEqualTo(itemId);
        assertThat(response.venueId()).isEqualTo(venueId);
        assertThat(response.name()).isEqualTo("Pizza");
        assertThat(response.description()).isEqualTo("Cheese");
        assertThat(response.price()).isEqualByComparingTo("12.50");
        assertThat(response.status()).isEqualTo(MenuItemStatus.ACTIVE);
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
        verify(menuItemRepository).findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE);
    }

    @Test
    void getReturnsNotFoundForMissingCrossVenueOrDeletedItem() {
        // Arrange
        when(menuItemRepository.findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> getMenuItemUseCase.execute(userId, venueId, itemId));

        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
        verify(menuItemRepository).findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE);
    }

    @Test
    void getStopsWhenManagerAuthorizationFails() {
        // Arrange
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);

        // Act & Assert
        assertThrows(AccessDeniedException.class,
                () -> getMenuItemUseCase.execute(userId, venueId, itemId));

        verifyNoInteractions(menuItemRepository);
    }

    private MenuItemEntity menuItem() {
        return menuItem(itemId, "Pizza", "Cheese");
    }

    private MenuItemEntity menuItem(UUID id, String name, String description) {
        MenuItemEntity item = MenuItemEntity.create(
                venueId, name, description, Money.of("12.50"), 15, MenuItemStatus.ACTIVE);
        item.setId(id);
        return item;
    }
}