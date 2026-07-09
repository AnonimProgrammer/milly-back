package com.milly.menu.application.usecase;

import com.milly.common.domain.valueobject.Money;
import com.milly.menu.application.dto.CreateMenuItemRequest;
import com.milly.menu.application.dto.MenuItemResponse;
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
public class CreateMenuItemUseCase {

    private final VenueAuthorizationService venueAuthorizationService;
    private final MenuItemJpaRepository menuItemRepository;

    @Transactional
    public MenuItemResponse execute(UUID userId, UUID venueId, CreateMenuItemRequest request) {
        venueAuthorizationService.requireRole(userId, venueId, VenueRole.MANAGER);

        MenuItemEntity menuItem = MenuItemEntity.create(
                venueId,
                request.name(),
                request.description(),
                Money.of(request.price()),
                request.approximatePreparationMinutes(),
                MenuItemStatus.ACTIVE);
        return MenuItemResponse.of(menuItemRepository.save(menuItem));
    }
}
