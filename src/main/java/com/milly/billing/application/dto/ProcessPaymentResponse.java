package com.milly.billing.application.dto;

public record ProcessPaymentResponse(
        PaymentResponse payment,
        BillSummaryResponse bill
) {
}
