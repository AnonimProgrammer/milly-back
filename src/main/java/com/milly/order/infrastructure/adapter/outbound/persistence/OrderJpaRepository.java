package com.milly.order.infrastructure.adapter.outbound.persistence;

import com.milly.order.domain.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    List<OrderEntity> findAllByTableIdOrderByCreatedAtDesc(UUID tableId);

    java.util.Optional<OrderEntity> findByIdAndTableId(UUID id, UUID tableId);
}
