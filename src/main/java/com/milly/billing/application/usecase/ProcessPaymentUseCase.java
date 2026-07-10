package com.milly.billing.application.usecase;

import com.github.f4b6a3.ulid.UlidCreator;
import com.milly.billing.application.dto.BillSummaryResponse;
import com.milly.billing.application.dto.CreatePaymentRequest;
import com.milly.billing.application.dto.PaymentResponse;
import com.milly.billing.application.dto.ProcessPaymentResponse;
import com.milly.billing.domain.entity.PaymentEntity;
import com.milly.billing.domain.valueobject.PaymentProvider;
import com.milly.billing.domain.valueobject.PaymentStatus;
import com.milly.billing.domain.valueobject.PaymentType;
import com.milly.billing.infrastructure.adapter.outbound.persistence.PaymentJpaRepository;
import com.milly.common.domain.valueobject.Money;
import com.milly.common.application.exception.PaymentValidationException;
import com.milly.common.application.exception.ResourceNotFoundException;
import com.milly.order.application.service.OrderEventNotifier;
import com.milly.order.application.service.OrderTotalCalculator;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.entity.OrderItemEntity;
import com.milly.order.domain.valueobject.OrderStatus;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCase {

    private final TableJpaRepository tableRepository;
    private final OrderJpaRepository orderRepository;
    private final OrderItemJpaRepository orderItemRepository;
    private final PaymentJpaRepository paymentRepository;
    private final OrderEventNotifier orderEventNotifier;

    @Transactional
    public ProcessPaymentResponse execute(UUID tableId, UUID orderId, CreatePaymentRequest request) {
        TableEntity table = tableRepository.findById(tableId)
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

        List<OrderItemEntity> items = orderItemRepository.findAllByOrderId(order.getId());
        BigDecimal orderTotal = OrderTotalCalculator.totalOf(items);

        List<PaymentEntity> existingPayments = paymentRepository
                .findAllByOrderIdAndStatusOrderByCreatedAtAsc(order.getId(), PaymentStatus.COMPLETED);
        BigDecimal paidAmount = sumAmounts(existingPayments);
        BigDecimal remaining = orderTotal.subtract(paidAmount).max(BigDecimal.ZERO);

        BigDecimal amount = request.amount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("Amount must be greater than zero.");
        }
        if (amount.compareTo(remaining) > 0) {
            throw new PaymentValidationException("Amount exceeds the remaining balance.");
        }

        BigDecimal tipAmount = request.tipAmount() != null ? request.tipAmount() : BigDecimal.ZERO;
        if (tipAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new PaymentValidationException("Tip amount must not be negative.");
        }
        if (tipAmount.compareTo(amount) > 0) {
            throw new PaymentValidationException("Tip amount exceeds the payment amount.");
        }

        validateProviderDetails(request);

        PaymentEntity payment = PaymentEntity.create(
                order.getId(),
                Money.of(amount),
                Money.of(tipAmount),
                request.provider(),
                request.paymentType(),
                generateProviderReference(),
                buildProviderMetadata(request));
        PaymentEntity savedPayment = paymentRepository.save(payment);
        orderEventNotifier.paymentReceived(order.getId(), table.getVenueId(), table.getId());

        List<PaymentEntity> allPayments = new ArrayList<>(existingPayments);
        allPayments.add(savedPayment);
        BillSummaryResponse bill = BillSummaryResponse.of(orderTotal, paidAmount.add(amount), allPayments);

        return new ProcessPaymentResponse(PaymentResponse.of(savedPayment), bill);
    }

    private void validateProviderDetails(CreatePaymentRequest request) {
        if (request.provider() == PaymentProvider.CARD) {
            CreatePaymentRequest.ProviderDetails details = request.providerDetails();
            if (details == null
                    || details.last4() == null || !details.last4().matches("\\d{4}")
                    || details.brand() == null || details.brand().isBlank()) {
                throw new PaymentValidationException("Card payments require a 4-digit last4 and a brand.");
            }
        }

        if (request.paymentType() == PaymentType.SPLIT
                && (request.splitPeople() == null || request.splitPeople() < 2)) {
            throw new PaymentValidationException(
                    "splitPeople is required and must be at least 2 for split payments.");
        }
    }

    private Map<String, Object> buildProviderMetadata(CreatePaymentRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        CreatePaymentRequest.ProviderDetails details = request.providerDetails();
        if (details != null) {
            if (details.last4() != null) {
                metadata.put("last4", details.last4());
            }
            if (details.brand() != null) {
                metadata.put("brand", details.brand());
            }
            if (details.expiryMonth() != null) {
                metadata.put("expiryMonth", details.expiryMonth());
            }
            if (details.expiryYear() != null) {
                metadata.put("expiryYear", details.expiryYear());
            }
        }
        if (request.paymentType() == PaymentType.SPLIT && request.splitPeople() != null) {
            metadata.put("splitPeople", request.splitPeople());
        }
        return metadata;
    }

    private String generateProviderReference() {
        return "pay_" + UlidCreator.getUlid().toString().substring(18).toLowerCase();
    }

    private BigDecimal sumAmounts(List<PaymentEntity> payments) {
        return payments.stream()
                .map(payment -> payment.getAmount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}