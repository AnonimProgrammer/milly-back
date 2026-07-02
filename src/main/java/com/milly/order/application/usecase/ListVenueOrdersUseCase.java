package com.milly.order.application.usecase;

import com.milly.order.application.dto.StaffOrderResponse;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.entity.OrderItemEntity;
import com.milly.order.domain.valueobject.OrderStatus;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.venue.application.service.VenueAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListVenueOrdersUseCase {

    private final VenueAuthorizationService venueAuthorizationService;
    private final OrderJpaRepository orderRepository;
    private final OrderItemJpaRepository orderItemRepository;

    @Transactional(readOnly = true)
    public List<StaffOrderResponse> execute(UUID venueId, UUID userId, OrderStatus status) {
        venueAuthorizationService.requireMember(userId, venueId);

        List<OrderEntity> orders = status != null
                ? orderRepository.findAllByVenueIdAndStatusOrderByCreatedAtDesc(venueId, status)
                : orderRepository.findAllByVenueIdOrderByCreatedAtDesc(venueId);

        Map<UUID, List<OrderItemEntity>> itemsByOrder = orderItemRepository
                .findAllByOrderIdIn(orders.stream().map(OrderEntity::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(OrderItemEntity::getOrderId));

        return orders.stream()
                .map(order -> StaffOrderResponse.of(order, itemsByOrder.getOrDefault(order.getId(), List.of())))
                .toList();
    }
}