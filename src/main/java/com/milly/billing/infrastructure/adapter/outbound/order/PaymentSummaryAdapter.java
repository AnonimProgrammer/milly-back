package com.milly.billing.infrastructure.adapter.outbound.order;

import com.milly.billing.infrastructure.adapter.outbound.persistence.OrderPaidAmountProjection;
import com.milly.billing.infrastructure.adapter.outbound.persistence.PaymentJpaRepository;
import com.milly.order.application.port.outbound.PaymentSummaryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Billing's implementation of the order module's {@link PaymentSummaryPort}. This is the one
 * place billing reaches "into" order's contract, not the other way around, keeping order free
 * of any compile-time dependency on billing.
 */
@Component
@RequiredArgsConstructor
public class PaymentSummaryAdapter implements PaymentSummaryPort {

    private final PaymentJpaRepository paymentRepository;

    @Override
    public BigDecimal paidAmountFor(UUID orderId) {
        return paymentRepository.sumCompletedAmountByOrderId(orderId);
    }

    @Override
    public Map<UUID, BigDecimal> paidAmountsFor(List<UUID> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return paymentRepository.sumCompletedAmountsByOrderIds(orderIds).stream()
                .collect(Collectors.toMap(
                        OrderPaidAmountProjection::getOrderId,
                        OrderPaidAmountProjection::getPaidAmount));
    }
}
