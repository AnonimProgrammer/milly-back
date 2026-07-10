package com.milly.billing.infrastructure.adapter.inbound.http;

import com.milly.billing.application.polluter.BillingPolluter;
import com.milly.billing.application.polluter.PayableOrder;
import com.milly.billing.application.polluter.UnpayableOrder;
import com.milly.billing.domain.valueobject.PaymentStatus;
import com.milly.billing.infrastructure.adapter.inbound.http.dto.BillSummaryApiResponse;
import com.milly.billing.infrastructure.adapter.inbound.http.dto.ProcessPaymentApiResponse;
import com.milly.billing.infrastructure.adapter.outbound.persistence.PaymentJpaRepository;
import com.milly.common.idempotency.IdempotencyAspect;
import com.milly.config.domain.AbstractITest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

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

    @Test
    void partialPaymentsAccumulateUntilFullyPaid() {
        // Arrange
        PayableOrder order = billingPolluter.createApprovedOrder();
        payFull(order, "60.00");

        // Act
        ProcessPaymentApiResponse response = restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders/{orderId}/payments", order.tableId(), order.orderId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", "40.00", "paymentType", "FULL", "provider", "GOOGLE"))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(ProcessPaymentApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().bill().paidAmount()).isEqualByComparingTo("100.00");
        assertThat(response.getData().bill().remaining()).isEqualByComparingTo("0.00");
        assertThat(response.getData().bill().fullyPaid()).isTrue();
        assertThat(paymentRepository.findAllByOrderIdAndStatusOrderByCreatedAtAsc(order.orderId(), PaymentStatus.COMPLETED))
                .hasSize(2);
    }
    @Test
    void processPaymentRejectsWhenOrderIsNotApproved() {
        // Arrange
        UnpayableOrder order = billingPolluter.createPendingOrder();

        // Act & Assert
        restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders/{orderId}/payments", order.tableId(), order.orderId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", "10.00", "paymentType", "FULL", "provider", "APPLE"))
                .exchange()
                .expectStatus()
                .isEqualTo(422)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("UNPROCESSABLE_ENTITY");

        assertThat(paymentRepository.findAllByOrderIdAndStatusOrderByCreatedAtAsc(order.orderId(), PaymentStatus.COMPLETED))
                .isEmpty();
    }

    @Test
    void processPaymentRejectsOverpayment() {
        // Arrange
        PayableOrder order = billingPolluter.createApprovedOrder();

        // Act & Assert
        restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders/{orderId}/payments", order.tableId(), order.orderId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", "999999.00", "paymentType", "FULL", "provider", "APPLE"))
                .exchange()
                .expectStatus()
                .isEqualTo(422)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("UNPROCESSABLE_ENTITY");

        assertThat(paymentRepository.findAllByOrderIdAndStatusOrderByCreatedAtAsc(order.orderId(), PaymentStatus.COMPLETED))
                .isEmpty();
    }

    @Test
    void processPaymentRejectsCardPaymentMissingCardDetails() {
        // Arrange
        PayableOrder order = billingPolluter.createApprovedOrder();

        // Act & Assert
        restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders/{orderId}/payments", order.tableId(), order.orderId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", "10.00", "paymentType", "FULL", "provider", "CARD"))
                .exchange()
                .expectStatus()
                .isEqualTo(422)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("UNPROCESSABLE_ENTITY");
    }

    @Test
    void processPaymentReturnsNotFoundWhenOrderBelongsToDifferentTable() {
        // Arrange
        PayableOrder order = billingPolluter.createApprovedOrder();
        UUID otherTableId = billingPolluter.createApprovedOrder().tableId();

        // Act & Assert
        restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders/{orderId}/payments", otherTableId, order.orderId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", "10.00", "paymentType", "FULL", "provider", "APPLE"))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("NOT_FOUND");
    }
    @Test
    void retryingPaymentWithSameIdempotencyKeyAndBodyDoesNotChargeTwice() {
        // Arrange
        PayableOrder order = billingPolluter.createApprovedOrder();
        String idempotencyKey = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of("amount", "25.00", "paymentType", "FULL", "provider", "APPLE");

        // Act
        ProcessPaymentApiResponse first = restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders/{orderId}/payments", order.tableId(), order.orderId())
                .header(IdempotencyAspect.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(ProcessPaymentApiResponse.class)
                .returnResult()
                .getResponseBody();

        ProcessPaymentApiResponse second = restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders/{orderId}/payments", order.tableId(), order.orderId())
                .header(IdempotencyAspect.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(ProcessPaymentApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(second.getData().payment().id()).isEqualTo(first.getData().payment().id());
        assertThat(paymentRepository.findAllByOrderIdAndStatusOrderByCreatedAtAsc(order.orderId(), PaymentStatus.COMPLETED))
                .singleElement();
    }

    @Test
    void reusingIdempotencyKeyWithDifferentBodyReturnsConflict() {
        // Arrange
        PayableOrder order = billingPolluter.createApprovedOrder();
        String idempotencyKey = UUID.randomUUID().toString();

        restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders/{orderId}/payments", order.tableId(), order.orderId())
                .header(IdempotencyAspect.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", "25.00", "paymentType", "FULL", "provider", "APPLE"))
                .exchange()
                .expectStatus()
                .isCreated();

        // Act & Assert
        restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders/{orderId}/payments", order.tableId(), order.orderId())
                .header(IdempotencyAspect.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", "30.00", "paymentType", "FULL", "provider", "APPLE"))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("CONFLICT");

        assertThat(paymentRepository.findAllByOrderIdAndStatusOrderByCreatedAtAsc(order.orderId(), PaymentStatus.COMPLETED))
                .singleElement();
    }
    @Test
    void getBillReturnsSummaryWithPaymentHistory() {
        // Arrange
        PayableOrder order = billingPolluter.createApprovedOrder();
        payFull(order, "42.50");

        // Act
        BillSummaryApiResponse response = restClient.get()
                .uri("/api/v1/public/tables/{tableId}/orders/{orderId}/bill", order.tableId(), order.orderId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(BillSummaryApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Bill retrieved successfully.");
        assertThat(response.getData().orderTotal()).isEqualByComparingTo(order.orderTotal());
        assertThat(response.getData().paidAmount()).isEqualByComparingTo("42.50");
        assertThat(response.getData().remaining()).isEqualByComparingTo("57.50");
        assertThat(response.getData().payments()).hasSize(1);
    }

    @Test
    void getBillReturnsNotFoundWhenOrderDoesNotExist() {
        // Arrange
        PayableOrder order = billingPolluter.createApprovedOrder();
        UUID missingOrderId = UUID.randomUUID();

        // Act & Assert
        restClient.get()
                .uri("/api/v1/public/tables/{tableId}/orders/{orderId}/bill", order.tableId(), missingOrderId)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("NOT_FOUND");
    }
    private void payFull(PayableOrder order, String amount) {
        restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders/{orderId}/payments", order.tableId(), order.orderId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amount, "paymentType", "CUSTOM", "provider", "APPLE"))
                .exchange()
                .expectStatus()
                .isCreated();
    }
}