package com.milly.menu.application.usecase;

import com.milly.common.domain.valueobject.Money;
import com.milly.common.exception.AccessDeniedException;
import com.milly.common.exception.ResourceNotFoundException;
import com.milly.menu.application.dto.CreateMenuItemRequest;
import com.milly.menu.application.dto.MenuItemResponse;
import com.milly.menu.application.dto.UpdateMenuItemRequest;
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

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuItemMutationUseCasesTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    @Mock
    private MenuItemJpaRepository menuItemRepository;

    private CreateMenuItemUseCase createMenuItemUseCase;
    private UpdateMenuItemUseCase updateMenuItemUseCase;
    private DeleteMenuItemUseCase deleteMenuItemUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        createMenuItemUseCase = new CreateMenuItemUseCase(venueAuthorizationService, menuItemRepository);
        updateMenuItemUseCase = new UpdateMenuItemUseCase(venueAuthorizationService, menuItemRepository);
        deleteMenuItemUseCase = new DeleteMenuItemUseCase(venueAuthorizationService, menuItemRepository);
    }

    @Test
    void createPersistsTrimmedActiveItem() {
        when(menuItemRepository.save(any(MenuItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MenuItemResponse response = createMenuItemUseCase.execute(
                userId, venueId, new CreateMenuItemRequest(" Pizza ", " Cheese ", new BigDecimal("12.50")));

        assertEquals("Pizza", response.name());
        assertEquals("Cheese", response.description());
        assertEquals(MenuItemStatus.ACTIVE, response.status());
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
    }

    @Test
    void createStopsWhenManagerAuthorizationFails() {
        denyManager();

        assertThrows(AccessDeniedException.class, () -> createMenuItemUseCase.execute(
                userId, venueId, new CreateMenuItemRequest("Pizza", null, BigDecimal.ONE)));

        verifyNoInteractions(menuItemRepository);
    }

    @Test
    void updateChangesOnlyProvidedFields() {
        MenuItemEntity item = menuItem();
        when(menuItemRepository.findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE))
                .thenReturn(Optional.of(item));
        when(menuItemRepository.save(item)).thenReturn(item);

        MenuItemResponse response = updateMenuItemUseCase.execute(
                userId, venueId, itemId, new UpdateMenuItemRequest(" Pasta ", null, null));

        assertEquals("Pasta", response.name());
        assertEquals("Cheese", response.description());
        assertEquals(new BigDecimal("12.50"), response.price());
    }

    @Test
    void updateReturnsNotFoundForMissingCrossVenueOrDeletedItem() {
        when(menuItemRepository.findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> updateMenuItemUseCase.execute(
                userId, venueId, itemId, new UpdateMenuItemRequest("Pasta", null, null)));
    }

    @Test
    void updateStopsWhenManagerAuthorizationFails() {
        denyManager();

        assertThrows(AccessDeniedException.class, () -> updateMenuItemUseCase.execute(
                userId, venueId, itemId, new UpdateMenuItemRequest("Pasta", null, null)));

        verifyNoInteractions(menuItemRepository);
    }

    @Test
    void deleteMarksItemDeletedWithoutRemovingIt() {
        MenuItemEntity item = menuItem();
        when(menuItemRepository.findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE))
                .thenReturn(Optional.of(item));
        when(menuItemRepository.save(item)).thenReturn(item);

        deleteMenuItemUseCase.execute(userId, venueId, itemId);

        assertEquals(MenuItemStatus.DELETED, item.getStatus());
        verify(menuItemRepository).save(item);
    }

    @Test
    void deleteReturnsNotFoundForMissingCrossVenueOrDeletedItem() {
        when(menuItemRepository.findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> deleteMenuItemUseCase.execute(userId, venueId, itemId));
    }

    @Test
    void deleteStopsWhenManagerAuthorizationFails() {
        denyManager();

        assertThrows(AccessDeniedException.class,
                () -> deleteMenuItemUseCase.execute(userId, venueId, itemId));

        verifyNoInteractions(menuItemRepository);
    }

    private void denyManager() {
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
    }

    private MenuItemEntity menuItem() {
        MenuItemEntity item = MenuItemEntity.create(
                venueId, "Pizza", "Cheese", Money.of("12.50"), MenuItemStatus.ACTIVE);
        item.setId(itemId);
        return item;
    }
}
