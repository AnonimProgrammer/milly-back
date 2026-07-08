package com.milly.order.infrastructure.adapter.outbound.persistence;

import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.valueobject.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    List<OrderEntity> findAllByTableIdOrderByCreatedAtDesc(UUID tableId);

    Optional<OrderEntity> findByIdAndTableId(UUID id, UUID tableId);

    List<OrderEntity> findAllByVenueIdOrderByCreatedAtDesc(UUID venueId);

    List<OrderEntity> findAllByVenueIdAndStatusOrderByCreatedAtDesc(UUID venueId, OrderStatus status);

    Optional<OrderEntity> findByIdAndVenueId(UUID id, UUID venueId);

    /**
     * Locks the order row for the duration of the transaction so two concurrent payment
     * attempts against the same order can't both read the same "remaining" balance and
     * jointly overpay it.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id AND o.tableId = :tableId")
    Optional<OrderEntity> findByIdAndTableIdForUpdate(@Param("id") UUID id, @Param("tableId") UUID tableId);
}
