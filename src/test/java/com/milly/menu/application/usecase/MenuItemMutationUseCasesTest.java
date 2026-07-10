package com.milly.menu.application.usecase;

import com.milly.common.domain.valueobject.Money;
import com.milly.common.application.exception.AccessDeniedException;
import com.milly.common.application.exception.ResourceNotFoundException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
        // Arrange
        when(menuItemRepository.save(any(MenuItemEntity.class)))
                .thenAnswer(invocation -> {
                    MenuItemEntity savedItem = invocation.getArgument(0);
                    savedItem.setId(itemId);
                    return savedItem;
                });

        // Act
        MenuItemResponse response = createMenuItemUseCase.execute(
                userId, venueId, new CreateMenuItemRequest(" Pizza ", " Cheese ", new BigDecimal("12.50"), 20));

        // Assert
        assertThat(response.id()).isEqualTo(itemId);
        assertThat(response.venueId()).isEqualTo(venueId);
        assertThat(response.name()).isEqualTo("Pizza");
        assertThat(response.description()).isEqualTo("Cheese");
        assertThat(response.price()).isEqualByComparingTo("12.50");
        assertThat(response.approximatePreparationMinutes()).isEqualTo(20);
        assertThat(response.status()).isEqualTo(MenuItemStatus.ACTIVE);
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
        verify(menuItemRepository).save(any(MenuItemEntity.class));
    }

    @Test
    void createPersistsNullDescriptionWhenDescriptionIsMissing() {
        // Arrange
        when(menuItemRepository.save(any(MenuItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        MenuItemResponse response = createMenuItemUseCase.execute(
                userId, venueId, new CreateMenuItemRequest("Pizza", null, new BigDecimal("12.50"), 15));

        // Assert
        assertThat(response.description()).isNull();
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
        verify(menuItemRepository).save(any(MenuItemEntity.class));
    }

    @Test
    void createStopsWhenManagerAuthorizationFails() {
        // Arrange
        denyManager();

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> createMenuItemUseCase.execute(
                userId, venueId, new CreateMenuItemRequest("Pizza", null, BigDecimal.ONE, 15)));

        verifyNoInteractions(menuItemRepository);
        verify(menuItemRepository, never()).save(any(MenuItemEntity.class));
    }

    @Test
    void updateChangesOnlyProvidedFields() {
        // Arrange
        MenuItemEntity item = menuItem();
        when(menuItemRepository.findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE))
                .thenReturn(Optional.of(item));
        when(menuItemRepository.save(item)).thenReturn(item);

        // Act
        MenuItemResponse response = updateMenuItemUseCase.execute(
                userId, venueId, itemId, new UpdateMenuItemRequest(" Pasta ", null, null, null));

        // Assert
        assertThat(response.name()).isEqualTo("Pasta");
        assertThat(response.description()).isEqualTo("Cheese");
        assertThat(response.price()).isEqualByComparingTo("12.50");
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
        verify(menuItemRepository).findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE);
        verify(menuItemRepository).save(item);
    }

    @Test
    void updateChangesAllProvidedFields() {
        // Arrange
        MenuItemEntity item = menuItem();
        when(menuItemRepository.findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE))
                .thenReturn(Optional.of(item));
        when(menuItemRepository.save(item)).thenReturn(item);

        // Act
        MenuItemResponse response = updateMenuItemUseCase.execute(
                userId,
                venueId,
                itemId,
                new UpdateMenuItemRequest(" Pasta ", " Tomato ", new BigDecimal("14.25"), null));

        // Assert
        assertThat(response.id()).isEqualTo(itemId);
        assertThat(response.venueId()).isEqualTo(venueId);
        assertThat(response.name()).isEqualTo("Pasta");
        assertThat(response.description()).isEqualTo("Tomato");
        assertThat(response.price()).isEqualByComparingTo("14.25");
        assertThat(response.status()).isEqualTo(MenuItemStatus.ACTIVE);
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
        verify(menuItemRepository).findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE);
        verify(menuItemRepository).save(item);
    }

    @Test
    void updateReturnsNotFoundForMissingCrossVenueOrDeletedItem() {
        // Arrange
        when(menuItemRepository.findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> updateMenuItemUseCase.execute(
                userId, venueId, itemId, new UpdateMenuItemRequest("Pasta", null, null, null)));

        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
        verify(menuItemRepository).findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE);
        verify(menuItemRepository, never()).save(any(MenuItemEntity.class));
    }

    @Test
    void updateStopsWhenManagerAuthorizationFails() {
        // Arrange
        denyManager();

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> updateMenuItemUseCase.execute(
                userId, venueId, itemId, new UpdateMenuItemRequest("Pasta", null, null, null)));

        verifyNoInteractions(menuItemRepository);
    }

    @Test
    void deleteMarksItemDeletedWithoutRemovingIt() {
        // Arrange
        MenuItemEntity item = menuItem();
        when(menuItemRepository.findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE))
                .thenReturn(Optional.of(item));
        when(menuItemRepository.save(item)).thenReturn(item);

        // Act
        deleteMenuItemUseCase.execute(userId, venueId, itemId);

        // Assert
        ArgumentCaptor<MenuItemEntity> itemCaptor = ArgumentCaptor.forClass(MenuItemEntity.class);
        assertThat(item.getStatus()).isEqualTo(MenuItemStatus.DELETED);
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
        verify(menuItemRepository).findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE);
        verify(menuItemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getId()).isEqualTo(itemId);
        assertThat(itemCaptor.getValue().getStatus()).isEqualTo(MenuItemStatus.DELETED);
    }

    @Test
    void deleteReturnsNotFoundForMissingCrossVenueOrDeletedItem() {
        // Arrange
        when(menuItemRepository.findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> deleteMenuItemUseCase.execute(userId, venueId, itemId));

        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
        verify(menuItemRepository).findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE);
        verify(menuItemRepository, never()).save(any(MenuItemEntity.class));
    }

    @Test
    void deleteStopsWhenManagerAuthorizationFails() {
        // Arrange
        denyManager();

        // Act & Assert
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
                venueId, "Pizza", "Cheese", Money.of("12.50"), 15, MenuItemStatus.ACTIVE);
        item.setId(itemId);
        return item;
    }
}