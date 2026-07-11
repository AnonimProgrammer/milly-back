package com.milly.billing.application.service;

import com.milly.billing.application.port.outbound.PaymentReceiptGenerator;
import com.milly.billing.domain.entity.PaymentEntity;
import com.milly.billing.domain.model.GeneratedReceipt;
import com.milly.config.application.port.outbound.BlobStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentReceiptService {

    private final PaymentReceiptGenerator paymentReceiptGenerator;
    private final BlobStorage blobStorage;

    public String generateAndStore(PaymentEntity payment, UUID venueId) {
        GeneratedReceipt receipt = paymentReceiptGenerator.generate(payment);

        String storageKey = "venues/%s/orders/%s/payments/%s/receipt.%s".formatted(
                venueId, payment.getOrderId(), payment.getId(), receipt.fileExtension());

        return blobStorage.upload(storageKey, receipt.content(), receipt.mimeType()).url();
    }
}
