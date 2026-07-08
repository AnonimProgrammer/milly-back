package com.milly.common.exception;

/**
 * Thrown for business-rule violations on a payment attempt (order not payable, invalid amount,
 * overpayment, missing provider details, etc). Mapped to HTTP 422 by {@code GlobalExceptionHandler}.
 */
public class PaymentValidationException extends RuntimeException {

    public PaymentValidationException(String message) {
        super(message);
    }
}
