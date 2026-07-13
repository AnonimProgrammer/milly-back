package com.milly.menu.application.usecase;

import com.milly.menu.application.dto.MenuItemResponse;
import com.milly.menu.domain.valueobject.MenuItemStatus;
import com.milly.menu.infrastructure.adapter.outbound.persistence.MenuItemJpaRepository;
import com.milly.venue.application.service.VenueAuthorizationService;
import com.milly.venue.domain.valueobject.VenueRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListMenuItemsUseCase {

    private final VenueAuthorizationService venueAuthorizationService;
    private final MenuItemJpaRepository menuItemRepository;

    @Transactional(readOnly = true)
    public List<MenuItemResponse> execute(UUID userId, UUID venueId) {
        venueAuthorizationService.requireAtLeastRole(userId, venueId, VenueRole.MANAGER);

        return menuItemRepository.findByVenueIdAndStatusOrderByCategoryAscNameAsc(venueId, MenuItemStatus.ACTIVE).stream()
                .map(MenuItemResponse::of)
                .toList();
    }
}
