package com.milly.billing.application.dto;

import com.milly.billing.domain.entity.PaymentEntity;
import com.milly.billing.domain.valueobject.PaymentProvider;
import com.milly.billing.domain.valueobject.PaymentStatus;
import com.milly.billing.domain.valueobject.PaymentType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        BigDecimal amount,
        BigDecimal tipAmount,
        PaymentStatus status,
        PaymentProvider provider,
        PaymentType paymentType,
        String providerReference,
        Map<String, Object> providerMetadata,
        OffsetDateTime createdAt
) {

    public static PaymentResponse of(PaymentEntity payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount().amount(),
                payment.getTipAmount().amount(),
                payment.getStatus(),
                payment.getProvider(),
                payment.getPaymentType(),
                payment.getProviderReference(),
                payment.getProviderMetadata(),
                payment.getCreatedAt());
    }
}