package com.milly.billing.domain.entity;

import com.github.f4b6a3.ulid.UlidCreator;
import com.milly.billing.domain.valueobject.PaymentProvider;
import com.milly.billing.domain.valueobject.PaymentStatus;
import com.milly.billing.domain.valueobject.PaymentType;
import com.milly.common.domain.valueobject.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    private UUID id = UlidCreator.getUlid().toUuid();

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false, precision = 12, scale = 2))
    })
    private Money amount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "tip_amount", nullable = false, precision = 12, scale = 2))
    })
    private Money tipAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType;

    @Column(name = "provider_reference")
    private String providerReference;

    // No explicit columnDefinition here so Hibernate maps this portably per-dialect
    // (jsonb on PostgreSQL via flyway migration V10, native JSON on H2 for tests).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_metadata")
    private Map<String, Object> providerMetadata;

    @Column(name = "failure_reason")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    /**
     * Creates a mock-processed, already-{@link PaymentStatus#COMPLETED} payment. There is no
     * real payment gateway in this flow: validation happens before this factory is called, so
     * every persisted payment is a successful attempt.
     */
    public static PaymentEntity create(
            UUID orderId,
            Money amount,
            Money tipAmount,
            PaymentProvider provider,
            PaymentType paymentType,
            String providerReference,
            Map<String, Object> providerMetadata) {
        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setTipAmount(tipAmount);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setProvider(provider);
        payment.setPaymentType(paymentType);
        payment.setProviderReference(providerReference);
        payment.setProviderMetadata(providerMetadata);
        return payment;
    }
}