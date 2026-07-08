package com.milly.billing.application.dto;

import com.milly.billing.domain.entity.PaymentEntity;

import java.math.BigDecimal;
import java.util.List;

public record BillSummaryResponse(
        BigDecimal orderTotal,
        BigDecimal paidAmount,
        BigDecimal remaining,
        boolean fullyPaid,
        List<PaymentResponse> payments
) {

    public static BillSummaryResponse of(BigDecimal orderTotal, BigDecimal paidAmount, List<PaymentEntity> payments) {
        BigDecimal remaining = orderTotal.subtract(paidAmount).max(BigDecimal.ZERO);
        boolean fullyPaid = paidAmount.compareTo(orderTotal) >= 0;
        return new BillSummaryResponse(
                orderTotal,
                paidAmount,
                remaining,
                fullyPaid,
                payments.stream().map(PaymentResponse::of).toList());
    }
}
