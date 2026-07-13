package com.milly.order.application.usecase;

import com.milly.common.application.exception.ResourceNotFoundException;
import com.milly.order.application.dto.StaffOrderResponse;
import com.milly.order.application.service.OrderEventNotifier;
import com.milly.order.application.service.OrderPreparationEstimator;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.entity.OrderItemEntity;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.venue.application.service.VenueAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApproveOrderUseCase {

    private final VenueAuthorizationService venueAuthorizationService;
    private final OrderJpaRepository orderRepository;
    private final OrderItemJpaRepository orderItemRepository;
    private final OrderEventNotifier orderEventNotifier;
    private final OrderPreparationEstimator orderPreparationEstimator;

    @Transactional
    public StaffOrderResponse execute(UUID venueId, UUID userId, UUID orderId) {
        venueAuthorizationService.requireActiveMember(userId, venueId);

        OrderEntity order = orderRepository.findByIdAndVenueId(orderId, venueId)
                .orElseThrow(ResourceNotFoundException::new);

        order.approve(OffsetDateTime.now());

        List<OrderItemEntity> orderItems = orderItemRepository.findAllByOrderId(order.getId());
        orderPreparationEstimator.tryEstimate(venueId, order.getId(), orderItems)
                .ifPresent(result -> order.applyPreparationEstimate(result.minutes(), result.value()));

        orderEventNotifier.orderApproved(order.getId(), order.getVenueId(), order.getTableId());

        return StaffOrderResponse.of(order, orderItems, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}