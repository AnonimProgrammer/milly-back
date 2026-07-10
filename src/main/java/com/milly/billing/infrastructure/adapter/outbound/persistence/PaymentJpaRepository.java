package com.milly.billing.infrastructure.adapter.outbound.persistence;

import com.milly.billing.domain.entity.PaymentEntity;
import com.milly.billing.domain.valueobject.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    List<PaymentEntity> findAllByOrderIdAndStatusOrderByCreatedAtAsc(UUID orderId, PaymentStatus status);

    @Query("""
            SELECT COALESCE(SUM(p.amount.amount), 0)
            FROM PaymentEntity p
            WHERE p.orderId = :orderId AND p.status = com.milly.billing.domain.valueobject.PaymentStatus.COMPLETED
            """)
    BigDecimal sumCompletedAmountByOrderId(@Param("orderId") UUID orderId);

    @Query("""
            SELECT COALESCE(SUM(p.tipAmount.amount), 0)
            FROM PaymentEntity p
            WHERE p.orderId = :orderId AND p.status = com.milly.billing.domain.valueobject.PaymentStatus.COMPLETED
            """)
    BigDecimal sumCompletedTipAmountByOrderId(@Param("orderId") UUID orderId);

    @Query("""
            SELECT p.orderId AS orderId, COALESCE(SUM(p.amount.amount), 0) AS paidAmount
            FROM PaymentEntity p
            WHERE p.status = com.milly.billing.domain.valueobject.PaymentStatus.COMPLETED
              AND p.orderId IN :orderIds
            GROUP BY p.orderId
            """)
    List<OrderPaidAmountProjection> sumCompletedAmountsByOrderIds(@Param("orderIds") List<UUID> orderIds);

    @Query("""
            SELECT p.orderId AS orderId, COALESCE(SUM(p.tipAmount.amount), 0) AS tipAmount
            FROM PaymentEntity p
            WHERE p.status = com.milly.billing.domain.valueobject.PaymentStatus.COMPLETED
              AND p.orderId IN :orderIds
            GROUP BY p.orderId
            """)
    List<OrderTipAmountProjection> sumCompletedTipAmountsByOrderIds(@Param("orderIds") List<UUID> orderIds);
}