package com.milly.billing.application.usecase;

import com.milly.billing.application.dto.CreatePaymentRequest;
import com.milly.billing.application.dto.ProcessPaymentResponse;
import com.milly.common.exception.PaymentValidationException;
import com.milly.common.exception.ResourceNotFoundException;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.valueobject.OrderStatus;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCase {

    private final TableJpaRepository tableRepository;
    private final OrderJpaRepository orderRepository;

    @Transactional
    public ProcessPaymentResponse execute(UUID tableId, UUID orderId, CreatePaymentRequest request) {
        tableRepository.findById(tableId)
                .filter(t -> t.getStatus() == TableStatus.ACTIVE)
                .orElseThrow(ResourceNotFoundException::new);

        // Pessimistic lock: serializes concurrent payment attempts against the same order so
        // two simultaneous requests can't both read the same remaining balance and jointly
        // overpay it.
        OrderEntity order = orderRepository.findByIdAndTableIdForUpdate(orderId, tableId)
                .orElseThrow(ResourceNotFoundException::new);

        if (order.getStatus() != OrderStatus.APPROVED) {
            throw new PaymentValidationException("Order is not open for payment.");
        }

        // TODO: Implement amount verification, provider validation, and persistence in subsequent commits
        return null;
    }
}
