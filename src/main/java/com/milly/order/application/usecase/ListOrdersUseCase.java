package com.milly.order.application.usecase;

import com.milly.common.exception.ResourceNotFoundException;
import com.milly.order.application.dto.OrderResponse;
import com.milly.order.application.port.outbound.PaymentSummaryPort;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListOrdersUseCase {

    private final TableJpaRepository tableRepository;
    private final OrderJpaRepository orderRepository;
    private final OrderItemJpaRepository orderItemRepository;
    private final PaymentSummaryPort paymentSummaryPort;

    @Transactional(readOnly = true)
    public List<OrderResponse> execute(UUID tableId) {
        tableRepository.findById(tableId)
                .filter(t -> t.getStatus() == TableStatus.ACTIVE)
                .orElseThrow(ResourceNotFoundException::new);

        List<OrderEntity> orders = orderRepository.findAllByTableIdOrderByCreatedAtDesc(tableId);
        List<UUID> orderIds = orders.stream().map(OrderEntity::getId).toList();
        Map<UUID, List<OrderItemEntity>> itemsByOrder = orderItemRepository.findAllByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItemEntity::getOrderId));
        Map<UUID, BigDecimal> paidAmountsByOrder = paymentSummaryPort.paidAmountsFor(orderIds);

        return orders.stream()
                .map(order -> OrderResponse.of(
                        order,
                        itemsByOrder.getOrDefault(order.getId(), List.of()),
                        paidAmountsByOrder.getOrDefault(order.getId(), BigDecimal.ZERO)))
                .toList();
    }
}
