package com.milly.order.application.usecase;

import com.milly.common.exception.ResourceNotFoundException;
import com.milly.order.application.dto.OrderResponse;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetOrderUseCase {

    private final TableJpaRepository tableRepository;
    private final OrderJpaRepository orderRepository;
    private final OrderItemJpaRepository orderItemRepository;

    public GetOrderUseCase(
            TableJpaRepository tableRepository,
            OrderJpaRepository orderRepository,
            OrderItemJpaRepository orderItemRepository) {
        this.tableRepository = tableRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public OrderResponse execute(UUID tableId, UUID orderId) {
        tableRepository.findById(tableId)
                .filter(t -> t.getStatus() == TableStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found."));

        OrderEntity order = orderRepository.findByIdAndTableId(orderId, tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

        return OrderResponse.of(order, orderItemRepository.findAllByOrderId(order.getId()));
    }
}
