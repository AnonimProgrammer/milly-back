package com.milly.billing.application.dto;

import com.milly.billing.domain.valueobject.PaymentProvider;
import com.milly.billing.domain.valueobject.PaymentType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Only structurally-required fields (missing entirely) are bean-validated here and result in a
 * 400. Business rules from the billing flow doc - amount <= 0, amount > remaining, missing card
 * details, invalid splitPeople - are all deliberately validated in {@code ProcessPaymentUseCase}
 * instead, so they consistently produce the 422 the doc calls for rather than a mix of 400/422.
 */
public record CreatePaymentRequest(
        @NotNull(message = "Amount is required.")
        BigDecimal amount,

        @NotNull(message = "Payment type is required.")
        PaymentType paymentType,

        @NotNull(message = "Provider is required.")
        PaymentProvider provider,

        ProviderDetails providerDetails,

        Integer splitPeople
) {

    public record ProviderDetails(
            String last4,
            String brand,
            Integer expiryMonth,
            Integer expiryYear
    ) {
    }
}