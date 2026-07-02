package com.milly.order.infrastructure.adapter.outbound.persistence;

import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.valueobject.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    List<OrderEntity> findAllByTableIdOrderByCreatedAtDesc(UUID tableId);

    Optional<OrderEntity> findByIdAndTableId(UUID id, UUID tableId);

    List<OrderEntity> findAllByVenueIdOrderByCreatedAtDesc(UUID venueId);

    List<OrderEntity> findAllByVenueIdAndStatusOrderByCreatedAtDesc(UUID venueId, OrderStatus status);

    Optional<OrderEntity> findByIdAndVenueId(UUID id, UUID venueId);
}
