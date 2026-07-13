package com.milly.order.application.usecase;

import com.milly.common.application.exception.ResourceNotFoundException;
import com.milly.order.application.dto.OrderPreparationEstimateResponse;
import com.milly.order.application.service.OrderPreparationEstimator;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.venue.application.service.VenueAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EstimateOrderPreparationTimeUseCase {

    private final VenueAuthorizationService venueAuthorizationService;
    private final OrderJpaRepository orderRepository;
    private final OrderItemJpaRepository orderItemRepository;
    private final OrderPreparationEstimator orderPreparationEstimator;

    @Transactional(readOnly = true)
    public OrderPreparationEstimateResponse execute(UUID venueId, UUID userId, UUID orderId) {
        venueAuthorizationService.requireActiveMember(userId, venueId);

        OrderEntity order = orderRepository.findByIdAndVenueId(orderId, venueId)
                .orElseThrow(ResourceNotFoundException::new);

        var orderItems = orderItemRepository.findAllByOrderId(order.getId());
        var aiResult = orderPreparationEstimator.estimate(venueId, order.getId(), orderItems);

        return OrderPreparationEstimateResponse.of(order.getId(), aiResult);
    }
}