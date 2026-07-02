package com.milly.menu.application.usecase;

import com.milly.common.exception.ResourceNotFoundException;
import com.milly.menu.application.dto.MenuItemResponse;
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
public class GetMenuItemUseCase {

    private final VenueAuthorizationService venueAuthorizationService;
    private final MenuItemJpaRepository menuItemRepository;

    @Transactional(readOnly = true)
    public MenuItemResponse execute(UUID userId, UUID venueId, UUID itemId) {
        venueAuthorizationService.requireRole(userId, venueId, VenueRole.MANAGER);

        return menuItemRepository.findByIdAndVenueIdAndStatus(itemId, venueId, MenuItemStatus.ACTIVE)
                .map(MenuItemResponse::of)
                .orElseThrow(ResourceNotFoundException::new);
    }
}
