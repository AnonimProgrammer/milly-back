package com.milly.order.application.usecase;

import com.milly.common.exception.InvalidStateTransitionException;
import com.milly.common.exception.ResourceNotFoundException;
import com.milly.order.application.dto.StaffOrderResponse;
import com.milly.order.application.port.outbound.PaymentSummaryPort;
import com.milly.order.application.service.OrderEventNotifier;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.valueobject.OrderStatus;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.venue.application.service.VenueAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloseOrderUseCase {

    private final VenueAuthorizationService venueAuthorizationService;
    private final OrderJpaRepository orderRepository;
    private final OrderItemJpaRepository orderItemRepository;
    private final OrderEventNotifier orderEventNotifier;
    private final PaymentSummaryPort paymentSummaryPort;

    @Transactional
    public StaffOrderResponse execute(UUID venueId, UUID userId, UUID orderId) {
        venueAuthorizationService.requireMember(userId, venueId);

        OrderEntity order = orderRepository.findByIdAndVenueId(orderId, venueId)
                .orElseThrow(ResourceNotFoundException::new);

        if (order.getStatus() != OrderStatus.APPROVED) {
            throw new InvalidStateTransitionException();
        }

        order.setStatus(OrderStatus.CLOSED);
        order.setClosedAt(OffsetDateTime.now());

        orderEventNotifier.orderClosed(order.getId(), order.getVenueId(), order.getTableId());

        // Staff can close an order regardless of how much (if anything) has been paid,
        // so this must reflect whatever payments actually exist.
        BigDecimal paidAmount = paymentSummaryPort.paidAmountFor(order.getId());
        return StaffOrderResponse.of(order, orderItemRepository.findAllByOrderId(order.getId()), paidAmount);
    }
}