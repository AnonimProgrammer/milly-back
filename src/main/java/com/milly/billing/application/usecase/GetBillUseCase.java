package com.milly.billing.application.usecase;

import com.milly.billing.application.dto.BillSummaryResponse;
import com.milly.billing.domain.entity.PaymentEntity;
import com.milly.billing.domain.valueobject.PaymentStatus;
import com.milly.billing.infrastructure.adapter.outbound.persistence.PaymentJpaRepository;
import com.milly.common.exception.ResourceNotFoundException;
import com.milly.order.application.service.OrderTotalCalculator;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.entity.OrderItemEntity;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetBillUseCase {

    private final TableJpaRepository tableRepository;
    private final OrderJpaRepository orderRepository;
    private final OrderItemJpaRepository orderItemRepository;
    private final PaymentJpaRepository paymentRepository;

    @Transactional(readOnly = true)
    public BillSummaryResponse execute(UUID tableId, UUID orderId) {
        tableRepository.findById(tableId)
                .filter(t -> t.getStatus() == TableStatus.ACTIVE)
                .orElseThrow(ResourceNotFoundException::new);

        OrderEntity order = orderRepository.findByIdAndTableId(orderId, tableId)
                .orElseThrow(ResourceNotFoundException::new);

        List<OrderItemEntity> items = orderItemRepository.findAllByOrderId(order.getId());
        BigDecimal orderTotal = OrderTotalCalculator.totalOf(items);

        List<PaymentEntity> payments = paymentRepository
                .findAllByOrderIdAndStatusOrderByCreatedAtAsc(order.getId(), PaymentStatus.COMPLETED);
        BigDecimal paidAmount = payments.stream()
                .map(payment -> payment.getAmount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return BillSummaryResponse.of(orderTotal, paidAmount, payments);
    }
}
