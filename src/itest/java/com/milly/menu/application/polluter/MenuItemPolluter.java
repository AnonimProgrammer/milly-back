package com.milly.menu.application.polluter;

import com.milly.common.domain.valueobject.Money;
import com.milly.menu.domain.entity.MenuItemEntity;
import com.milly.menu.domain.valueobject.MenuItemStatus;
import com.milly.menu.infrastructure.adapter.outbound.persistence.MenuItemJpaRepository;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MenuItemPolluter {

    private final MenuItemJpaRepository menuItemRepository;
    private final TableJpaRepository tableRepository;

    public MenuItemEntity createActiveItem(UUID venueId, String name) {
        return createItem(venueId, name, "Description", "12.50", MenuItemStatus.ACTIVE);
    }

    public MenuItemEntity createDeletedItem(UUID venueId, String name) {
        return createItem(venueId, name, "Description", "12.50", MenuItemStatus.DELETED);
    }

    public MenuItemEntity createItem(
            UUID venueId,
            String name,
            String description,
            String price,
            MenuItemStatus status) {
        return menuItemRepository.save(MenuItemEntity.create(
                venueId,
                name,
                description,
                Money.of(price),
                status));
    }

    public TableEntity createActiveTable(UUID venueId) {
        return createTable(venueId, TableStatus.ACTIVE);
    }

    public TableEntity createInactiveTable(UUID venueId) {
        return createTable(venueId, TableStatus.INACTIVE);
    }

    private TableEntity createTable(UUID venueId, TableStatus status) {
        return tableRepository.save(TableEntity.create(venueId, "T-" + UUID.randomUUID(), status));
    }
}
