package com.milly.billing.application.service;

import com.milly.billing.application.port.outbound.PaymentReceiptGenerator;
import com.milly.billing.domain.entity.PaymentEntity;
import com.milly.billing.domain.model.GeneratedReceipt;
import com.milly.config.application.port.outbound.BlobStorage;
import com.milly.config.domain.model.BlobObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static com.milly.billing.application.usecase.builder.PaymentTestBuilder.aPayment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReceiptServiceTest {

    @Mock
    private PaymentReceiptGenerator paymentReceiptGenerator;

    @Mock
    private BlobStorage blobStorage;

    @Test
    void generatesAndStoresReceiptUsingPaymentScopedKey() {
        // Arrange
        UUID venueId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentEntity payment = aPayment()
                .withId(paymentId)
                .withOrderId(orderId)
                .build();
        byte[] content = "receipt".getBytes();
        GeneratedReceipt receipt = new GeneratedReceipt(content, "application/pdf", "pdf");
        String storageKey = "venues/%s/orders/%s/payments/%s/receipt.pdf"
                .formatted(venueId, orderId, paymentId);
        String receiptUrl = "https://storage.example/receipt.pdf";
        when(paymentReceiptGenerator.generate(payment)).thenReturn(receipt);
        when(blobStorage.upload(storageKey, content, "application/pdf"))
                .thenReturn(new BlobObject(storageKey, content, "application/pdf", receiptUrl));

        // Act
        String result = new PaymentReceiptService(paymentReceiptGenerator, blobStorage)
                .generateAndStore(payment, venueId);

        // Assert
        assertThat(result).isEqualTo(receiptUrl);
        verify(paymentReceiptGenerator).generate(payment);
        verify(blobStorage).upload(storageKey, content, "application/pdf");
    }
}
