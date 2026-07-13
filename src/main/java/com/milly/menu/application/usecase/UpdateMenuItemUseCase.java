package com.milly.menu.application.usecase;

import com.milly.common.domain.valueobject.Money;
import com.milly.common.application.exception.ResourceNotFoundException;
import com.milly.menu.application.dto.MenuItemResponse;
import com.milly.menu.application.dto.UpdateMenuItemRequest;
import com.milly.menu.domain.entity.MenuItemEntity;
import com.milly.menu.domain.valueobject.MenuItemStatus;
import com.milly.menu.infrastructure.adapter.outbound.persistence.MenuItemJpaRepository;
import com.milly.venue.application.service.VenueAuthorizationService;
import com.milly.venue.domain.valueobject.VenueRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateMenuItemUseCase {

    private final VenueAuthorizationService venueAuthorizationService;
    private final MenuItemJpaRepository menuItemRepository;

    @Transactional
    public MenuItemResponse execute(UUID userId, UUID venueId, UUID itemId, UpdateMenuItemRequest request) {
        venueAuthorizationService.requireAtLeastRole(userId, venueId, VenueRole.MANAGER);

        MenuItemEntity menuItem = menuItemRepository
                .findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE)
                .orElseThrow(ResourceNotFoundException::new);

        if (request.name() != null) {
            menuItem.updateName(request.name());
        }
        if (request.description() != null) {
            menuItem.updateDescription(request.description());
        }
        if (request.price() != null) {
            menuItem.updatePrice(Money.of(request.price()));
        }
        if (request.approximatePreparationMinutes() != null) {
            menuItem.updateApproximatePreparationMinutes(request.approximatePreparationMinutes());
        }
        if (request.category() != null) {
            menuItem.updateCategory(request.category());
        }

        return MenuItemResponse.of(menuItemRepository.save(menuItem));
    }
}