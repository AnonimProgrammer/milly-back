package com.milly.billing.application.usecase.builder;

import com.milly.billing.domain.entity.PaymentEntity;
import com.milly.billing.domain.valueobject.PaymentProvider;
import com.milly.billing.domain.valueobject.PaymentStatus;
import com.milly.billing.domain.valueobject.PaymentType;
import com.milly.common.domain.valueobject.Money;

import java.util.Collections;
import java.util.UUID;

public final class PaymentTestBuilder {

    private UUID id = UUID.randomUUID();
    private UUID orderId = UUID.randomUUID();
    private Money amount = Money.of("100.00");
    private PaymentStatus status = PaymentStatus.COMPLETED;
    private PaymentProvider provider = PaymentProvider.APPLE;
    private PaymentType paymentType = PaymentType.FULL;

    private PaymentTestBuilder() {
    }

    public static PaymentTestBuilder aPayment() {
        return new PaymentTestBuilder();
    }

    public PaymentTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public PaymentTestBuilder withOrderId(UUID orderId) {
        this.orderId = orderId;
        return this;
    }

    public PaymentTestBuilder withAmount(Money amount) {
        this.amount = amount;
        return this;
    }

    public PaymentTestBuilder withStatus(PaymentStatus status) {
        this.status = status;
        return this;
    }

    public PaymentTestBuilder withProvider(PaymentProvider provider) {
        this.provider = provider;
        return this;
    }

    public PaymentTestBuilder withPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
        return this;
    }

    public PaymentEntity build() {
        PaymentEntity payment = PaymentEntity.create(
                orderId, amount, provider, paymentType, "ref_" + id, Collections.emptyMap());
        payment.setId(id);
        payment.setStatus(status);
        return payment;
    }
}