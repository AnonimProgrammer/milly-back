package com.milly.menu.application.usecase;

import com.milly.common.application.exception.ResourceNotFoundException;
import com.milly.menu.application.dto.MenuItemResponse;
import com.milly.menu.domain.valueobject.MenuItemStatus;
import com.milly.menu.infrastructure.adapter.outbound.persistence.MenuItemJpaRepository;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListPublicMenuItemsUseCase {

    private final TableJpaRepository tableRepository;
    private final MenuItemJpaRepository menuItemRepository;

    @Transactional(readOnly = true)
    public List<MenuItemResponse> execute(UUID tableId) {
        var table = tableRepository.findById(tableId)
                .filter(t -> t.getStatus() == TableStatus.ACTIVE)
                .orElseThrow(ResourceNotFoundException::new);

        return menuItemRepository.findByVenueIdAndStatusOrderByCategoryAscNameAsc(table.getVenueId(), MenuItemStatus.ACTIVE).stream()
                .map(MenuItemResponse::of)
                .toList();
    }
}
