package com.milly.order.application.usecase;

import com.milly.common.exception.ResourceNotFoundException;
import com.milly.order.application.dto.OrderResponse;
import com.milly.order.application.port.outbound.PaymentSummaryPort;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetOrderUseCase {

    private final TableJpaRepository tableRepository;
    private final OrderJpaRepository orderRepository;
    private final OrderItemJpaRepository orderItemRepository;
    private final PaymentSummaryPort paymentSummaryPort;

    @Transactional(readOnly = true)
    public OrderResponse execute(UUID tableId, UUID orderId) {
        tableRepository.findById(tableId)
                .filter(t -> t.getStatus() == TableStatus.ACTIVE)
                .orElseThrow(ResourceNotFoundException::new);

        OrderEntity order = orderRepository.findByIdAndTableId(orderId, tableId)
                .orElseThrow(ResourceNotFoundException::new);

        BigDecimal paidAmount = paymentSummaryPort.paidAmountFor(order.getId());
        return OrderResponse.of(order, orderItemRepository.findAllByOrderId(order.getId()), paidAmount);
    }
}