package com.milly.menu.infrastructure.adapter.outbound.persistence;

import com.milly.menu.domain.entity.MenuItemEntity;
import com.milly.menu.domain.valueobject.MenuItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuItemJpaRepository extends JpaRepository<MenuItemEntity, UUID> {

    List<MenuItemEntity> findByVenueIdAndStatusOrderByCategoryAscNameAsc(UUID venueId, MenuItemStatus status);

    List<MenuItemEntity> findAllByIdInAndVenueId(List<UUID> ids, UUID venueId);

    Optional<MenuItemEntity> findByIdAndVenueIdAndStatus(UUID id, UUID venueId, MenuItemStatus status);
}