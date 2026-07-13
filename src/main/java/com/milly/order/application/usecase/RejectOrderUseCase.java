package com.milly.order.application.usecase;

import com.milly.common.application.exception.ResourceNotFoundException;
import com.milly.order.application.dto.StaffOrderResponse;
import com.milly.order.application.service.OrderEventNotifier;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.venue.application.service.VenueAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RejectOrderUseCase {

    private final VenueAuthorizationService venueAuthorizationService;
    private final OrderJpaRepository orderRepository;
    private final OrderItemJpaRepository orderItemRepository;
    private final OrderEventNotifier orderEventNotifier;

    @Transactional
    public StaffOrderResponse execute(UUID venueId, UUID userId, UUID orderId) {
        venueAuthorizationService.requireActiveMember(userId, venueId);

        OrderEntity order = orderRepository.findByIdAndVenueId(orderId, venueId)
                .orElseThrow(ResourceNotFoundException::new);

        order.reject();

        orderEventNotifier.orderRejected(order.getId(), order.getVenueId(), order.getTableId());

        // Only reachable from PENDING, where payments can't exist yet.
        return StaffOrderResponse.of(order, orderItemRepository.findAllByOrderId(order.getId()), BigDecimal.ZERO, BigDecimal.ZERO);
    }
}