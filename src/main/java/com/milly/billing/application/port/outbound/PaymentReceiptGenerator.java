package com.milly.billing.application.port.outbound;

import com.milly.billing.domain.entity.PaymentEntity;
import com.milly.billing.domain.model.GeneratedReceipt;

public interface PaymentReceiptGenerator {

    GeneratedReceipt generate(PaymentEntity payment);
}
