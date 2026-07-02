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

    @BeforeEach
    void setUp() {
        listMenuItemsUseCase = new ListMenuItemsUseCase(venueAuthorizationService, menuItemRepository);
        getMenuItemUseCase = new GetMenuItemUseCase(venueAuthorizationService, menuItemRepository);
    }

    @Test
    void listReturnsOnlyRepositoryActiveItems() {
        MenuItemEntity item = menuItem();
        when(menuItemRepository.findByVenueIdAndStatusOrderByNameAsc(venueId, MenuItemStatus.ACTIVE))
                .thenReturn(List.of(item));

        List<MenuItemResponse> response = listMenuItemsUseCase.execute(userId, venueId);

        assertEquals(List.of(item.getId()), response.stream().map(MenuItemResponse::id).toList());
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
    }

    @Test
    void listStopsWhenManagerAuthorizationFails() {
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);

        assertThrows(AccessDeniedException.class, () -> listMenuItemsUseCase.execute(userId, venueId));

        verifyNoInteractions(menuItemRepository);
    }

    @Test
    void getReturnsActiveItemFromRequestedVenue() {
        MenuItemEntity item = menuItem();
        when(menuItemRepository.findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE))
                .thenReturn(Optional.of(item));

        MenuItemResponse response = getMenuItemUseCase.execute(userId, venueId, itemId);

        assertEquals(item.getId(), response.id());
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
    }

    @Test
    void getReturnsNotFoundForMissingCrossVenueOrDeletedItem() {
        when(menuItemRepository.findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> getMenuItemUseCase.execute(userId, venueId, itemId));
    }

    @Test
    void getStopsWhenManagerAuthorizationFails() {
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);

        assertThrows(AccessDeniedException.class,
                () -> getMenuItemUseCase.execute(userId, venueId, itemId));

        verifyNoInteractions(menuItemRepository);
    }

    private MenuItemEntity menuItem() {
        MenuItemEntity item = MenuItemEntity.create(
                venueId, "Pizza", "Cheese", Money.of("12.50"), MenuItemStatus.ACTIVE);
        item.setId(itemId);
        return item;
    }
}
