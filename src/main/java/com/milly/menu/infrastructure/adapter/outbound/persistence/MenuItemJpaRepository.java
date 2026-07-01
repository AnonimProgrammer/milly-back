package com.milly.menu.infrastructure.adapter.outbound.persistence;

import com.milly.menu.domain.entity.MenuItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MenuItemJpaRepository extends JpaRepository<MenuItemEntity, UUID> {
}
