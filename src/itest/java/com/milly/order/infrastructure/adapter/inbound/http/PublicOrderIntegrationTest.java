package com.milly.order.infrastructure.adapter.inbound.http;

import com.milly.config.domain.AbstractITest;
import com.milly.config.infrastructure.adapter.RestTestClientAuth;
import com.milly.order.application.dto.AddOrderItemsRequest;
import com.milly.order.application.dto.CreateOrderRequest;
import com.milly.order.application.polluter.OrderPolluter;
import com.milly.order.application.polluter.OrderTestFixture;
import com.milly.order.domain.valueobject.OrderStatus;
import com.milly.order.infrastructure.adapter.inbound.http.dto.OrderApiResponse;
import com.milly.order.infrastructure.adapter.inbound.http.dto.OrderListApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PublicOrderIntegrationTest extends AbstractITest {

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private OrderPolluter orderPolluter;

    @Test
    void guestPlacesOrder() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();

        // Act
        OrderApiResponse response = restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders", fixture.tableId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(createOrderRequest(fixture.menuItemId(), 1))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(OrderApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().tableId()).isEqualTo(fixture.tableId());
        assertThat(response.getData().status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getData().items()).hasSize(1);
        assertThat(response.getData().items().getFirst().menuItemId()).isEqualTo(fixture.menuItemId());
        assertThat(response.getData().items().getFirst().quantity()).isEqualTo(1);
        assertThat(response.getData().items().getFirst().unitPrice()).isEqualByComparingTo(new BigDecimal("12.50"));
    }

    @Test
    void listOrdersReturnsPlacedOrder() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();
        UUID orderId = placeOrder(fixture);

        // Act
        OrderListApiResponse response = restClient.get()
                .uri("/api/v1/public/tables/{tableId}/orders", fixture.tableId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OrderListApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData()).extracting("id").contains(orderId);
    }

    @Test
    void getOrderReturnsPlacedOrder() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();
        UUID orderId = placeOrder(fixture);

        // Act
        OrderApiResponse response = restClient.get()
                .uri("/api/v1/public/tables/{tableId}/orders/{orderId}", fixture.tableId(), orderId)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OrderApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().id()).isEqualTo(orderId);
        assertThat(response.getData().status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void placeOrderWithUnknownTableReturnsNotFound() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();

        // Act & Assert
        restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .body(createOrderRequest(fixture.menuItemId(), 1))
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void placeOrderWithUnknownMenuItemReturnsNotFound() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();

        // Act & Assert
        restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders", fixture.tableId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(createOrderRequest(UUID.randomUUID(), 1))
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void addItemsToApprovedOrder() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();
        UUID orderId = placeOrder(fixture);
        approveOrder(fixture, orderId);

        // Act
        OrderApiResponse response = restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders/{orderId}/items", fixture.tableId(), orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(addOrderItemsRequest(fixture.menuItemId(), 2))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OrderApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().status()).isEqualTo(OrderStatus.APPROVED);
        assertThat(response.getData().items()).hasSize(2);
    }

    @Test
    void addItemsToPendingOrderReturnsConflict() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();
        UUID orderId = placeOrder(fixture);

        // Act & Assert
        restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders/{orderId}/items", fixture.tableId(), orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(addOrderItemsRequest(fixture.menuItemId(), 1))
                .exchange()
                .expectStatus()
                .isEqualTo(409);
    }

    @Test
    void getOrderWithUnknownIdReturnsNotFound() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();

        // Act & Assert
        restClient.get()
                .uri("/api/v1/public/tables/{tableId}/orders/{orderId}", fixture.tableId(), UUID.randomUUID())
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void listOrdersReturnsEmptyListWhenTableHasNoOrders() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();

        // Act
        OrderListApiResponse response = restClient.get()
                .uri("/api/v1/public/tables/{tableId}/orders", fixture.tableId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OrderListApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData()).isEmpty();
    }

    @Test
    void placeOrderOnInactiveTableReturnsNotFound() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createInactiveOrderableTable();

        // Act & Assert
        restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders", fixture.tableId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(createOrderRequest(fixture.menuItemId(), 1))
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void addItemsToOrderWithUnknownMenuItemReturnsNotFound() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();
        UUID orderId = placeOrder(fixture);
        approveOrder(fixture, orderId);

        // Act & Assert
        restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders/{orderId}/items", fixture.tableId(), orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(addOrderItemsRequest(UUID.randomUUID(), 1))
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    private UUID placeOrder(OrderTestFixture fixture) {
        OrderApiResponse response = restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders", fixture.tableId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(createOrderRequest(fixture.menuItemId(), 1))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(OrderApiResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(response).isNotNull();
        return response.getData().id();
    }

    private void approveOrder(OrderTestFixture fixture, UUID orderId) {
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, fixture.venue().manager());
        managerClient.post()
                .uri("/api/v1/venues/{venueId}/orders/{orderId}/approve",
                        fixture.venue().venueId(), orderId)
                .exchange()
                .expectStatus()
                .isOk();
    }

    private static CreateOrderRequest createOrderRequest(UUID menuItemId, int quantity) {
        return new CreateOrderRequest(List.of(new CreateOrderRequest.ItemDto(menuItemId, quantity)));
    }

    private static AddOrderItemsRequest addOrderItemsRequest(UUID menuItemId, int quantity) {
        return new AddOrderItemsRequest(List.of(new CreateOrderRequest.ItemDto(menuItemId, quantity)));
    }
}
