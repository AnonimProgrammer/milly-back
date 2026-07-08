package com.milly.billing.infrastructure.adapter.inbound.http;

import com.milly.billing.application.polluter.BillingPolluter;
import com.milly.billing.application.polluter.PayableOrder;
import com.milly.billing.domain.valueobject.PaymentStatus;
import com.milly.billing.infrastructure.adapter.inbound.http.dto.ProcessPaymentApiResponse;
import com.milly.billing.infrastructure.adapter.outbound.persistence.PaymentJpaRepository;
import com.milly.config.domain.AbstractITest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRestIntegrationTest extends AbstractITest {

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private BillingPolluter billingPolluter;

    @Autowired
    private PaymentJpaRepository paymentRepository;

    @Test
    void processCardPaymentReturnsCreatedAndUpdatedBill() {
        // Arrange
        PayableOrder order = billingPolluter.createApprovedOrder();

        // Act
        ProcessPaymentApiResponse response = restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders/{orderId}/payments", order.tableId(), order.orderId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "amount", "42.50",
                        "paymentType", "SPLIT",
                        "provider", "CARD",
                        "providerDetails", Map.of("last4", "4242", "brand", "visa"),
                        "splitPeople", 4))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(ProcessPaymentApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getMessage()).isEqualTo("Payment processed successfully.");
        assertThat(response.getData().payment().status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.getData().payment().amount()).isEqualByComparingTo("42.50");
        assertThat(response.getData().payment().providerMetadata())
                .containsEntry("last4", "4242")
                .containsEntry("brand", "visa")
                .containsEntry("splitPeople", 4);
        assertThat(response.getData().bill().orderTotal()).isEqualByComparingTo(order.orderTotal());
        assertThat(response.getData().bill().paidAmount()).isEqualByComparingTo("42.50");
        assertThat(response.getData().bill().remaining()).isEqualByComparingTo("57.50");
        assertThat(response.getData().bill().fullyPaid()).isFalse();

        assertThat(paymentRepository.findAllByOrderIdAndStatusOrderByCreatedAtAsc(order.orderId(), PaymentStatus.COMPLETED))
                .singleElement()
                .satisfies(payment -> assertThat(payment.getAmount().amount()).isEqualByComparingTo("42.50"));
    }

    @Test
    void processWalletPaymentRequiresNoCardDetails() {
        // Arrange
        PayableOrder order = billingPolluter.createApprovedOrder();

        // Act
        ProcessPaymentApiResponse response = restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders/{orderId}/payments", order.tableId(), order.orderId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "amount", "100.00",
                        "paymentType", "FULL",
                        "provider", "APPLE"))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(ProcessPaymentApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().bill().remaining()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getData().bill().fullyPaid()).isTrue();
    }
}
