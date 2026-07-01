package com.milly.order.infrastructure.adapter.outbound.persistence;

import com.milly.order.domain.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderItemJpaRepository extends JpaRepository<OrderItemEntity, UUID> {

    List<OrderItemEntity> findAllByOrderId(UUID orderId);

    List<OrderItemEntity> findAllByOrderIdIn(List<UUID> orderIds);
}
