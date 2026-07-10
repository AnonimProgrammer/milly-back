package com.milly.order.infrastructure.adapter.inbound.http;

import com.milly.auth.application.polluter.AuthSession;
import com.milly.auth.application.polluter.AuthSessionPolluter;
import com.milly.config.domain.AbstractITest;
import com.milly.config.infrastructure.adapter.RestTestClientAuth;
import com.milly.order.application.dto.CreateOrderRequest;
import com.milly.order.application.polluter.OrderPolluter;
import com.milly.order.application.polluter.OrderTestFixture;
import com.milly.order.domain.valueobject.OrderStatus;
import com.milly.order.infrastructure.adapter.inbound.http.dto.OrderApiResponse;
import com.milly.order.infrastructure.adapter.inbound.http.dto.StaffOrderApiResponse;
import com.milly.order.infrastructure.adapter.inbound.http.dto.StaffOrderListApiResponse;
import com.milly.venue.application.polluter.ManagedVenue;
import com.milly.venue.application.polluter.VenuePolluter;
import com.milly.venue.domain.valueobject.VenueRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StaffOrderIntegrationTest extends AbstractITest {

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private OrderPolluter orderPolluter;

    @Autowired
    private VenuePolluter venuePolluter;

    @Autowired
    private AuthSessionPolluter authSessionPolluter;

    @Test
    void memberListsOrders() throws InterruptedException {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();
        UUID olderOrderId = placeOrder(fixture);
        Thread.sleep(20);
        UUID newerOrderId = placeOrder(fixture);
        // Act
        StaffOrderListApiResponse response = RestTestClientAuth.withSession(restClient, fixture.venue().manager()).get()
                .uri("/api/v1/venues/{venueId}/orders", fixture.venue().venueId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(StaffOrderListApiResponse.class)
                .returnResult().getResponseBody();
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().data()).extracting("id").containsExactly(olderOrderId, newerOrderId);
        assertThat(response.getData().pagination().limit()).isEqualTo(20);
    }

    @Test
    void memberListsOrdersWithLimit() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();
        placeOrder(fixture);
        placeOrder(fixture);
        ZoneId zone = ZoneId.of("UTC");
        OffsetDateTime from = LocalDate.now(zone).atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime to = LocalDate.now(zone).atTime(LocalTime.MAX).atZone(zone).toOffsetDateTime();
        // Act
        StaffOrderListApiResponse response = RestTestClientAuth.withSession(restClient, fixture.venue().manager()).get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/venues/{venueId}/orders")
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .queryParam("limit", 1)
                        .build(fixture.venue().venueId()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(StaffOrderListApiResponse.class)
                .returnResult().getResponseBody();
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().data()).hasSize(1);
        assertThat(response.getData().pagination().limit()).isEqualTo(1);
        assertThat(response.getData().pagination().hasNext()).isTrue();
        assertThat(response.getData().pagination().nextCursor()).isEqualTo("1");
    }

    @Test
    void memberGetsOrder() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();
        UUID orderId = placeOrder(fixture);
        // Act
        StaffOrderApiResponse response = RestTestClientAuth.withSession(restClient, fixture.venue().manager()).get()
                .uri("/api/v1/venues/{venueId}/orders/{orderId}", fixture.venue().venueId(), orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(StaffOrderApiResponse.class)
                .returnResult().getResponseBody();
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().id()).isEqualTo(orderId);
        assertThat(response.getData().status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void memberApprovesOrder() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();
        UUID orderId = placeOrder(fixture);
        // Act
        StaffOrderApiResponse response = RestTestClientAuth.withSession(restClient, fixture.venue().manager()).post()
                .uri("/api/v1/venues/{venueId}/orders/{orderId}/approve",
                        fixture.venue().venueId(), orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(StaffOrderApiResponse.class)
                .returnResult().getResponseBody();
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().status()).isEqualTo(OrderStatus.APPROVED);
    }

    @Test
    void memberRejectsOrder() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();
        UUID orderId = placeOrder(fixture);
        // Act
        StaffOrderApiResponse response = RestTestClientAuth.withSession(restClient, fixture.venue().manager()).post()
                .uri("/api/v1/venues/{venueId}/orders/{orderId}/reject",
                        fixture.venue().venueId(), orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(StaffOrderApiResponse.class)
                .returnResult().getResponseBody();
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().status()).isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    void memberClosesApprovedOrder() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();
        UUID orderId = placeOrder(fixture);
        approveOrder(RestTestClientAuth.withSession(restClient, fixture.venue().manager()), fixture, orderId);
        // Act
        StaffOrderApiResponse response = RestTestClientAuth.withSession(restClient, fixture.venue().manager()).post()
                .uri("/api/v1/venues/{venueId}/orders/{orderId}/close",
                        fixture.venue().venueId(), orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(StaffOrderApiResponse.class)
                .returnResult().getResponseBody();
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().status()).isEqualTo(OrderStatus.CLOSED);
        assertThat(response.getData().closedAt()).isNotNull();
    }

    @Test
    void listOrdersByStatusFilter() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();
        UUID pendingOrderId = placeOrder(fixture);
        UUID approvedOrderId = placeOrder(fixture);
        approveOrder(RestTestClientAuth.withSession(restClient, fixture.venue().manager()), fixture, approvedOrderId);
        // Act
        StaffOrderListApiResponse response = RestTestClientAuth.withSession(restClient, fixture.venue().manager()).get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/venues/{venueId}/orders")
                        .queryParam("status", OrderStatus.PENDING)
                        .build(fixture.venue().venueId()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(StaffOrderListApiResponse.class)
                .returnResult().getResponseBody();
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().data()).extracting("id").contains(pendingOrderId);
        assertThat(response.getData().data()).extracting("id").doesNotContain(approvedOrderId);
    }

    @Test
    void unauthenticatedReturnsUnauthorized() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();
        // Act & Assert
        restClient.get()
                .uri("/api/v1/venues/{venueId}/orders", fixture.venue().venueId())
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void nonMemberReturnsForbidden() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();
        AuthSession outsider = authSessionPolluter.registerPasswordUser();
        // Act & Assert
        RestTestClientAuth.withSession(restClient, outsider).get()
                .uri("/api/v1/venues/{venueId}/orders", fixture.venue().venueId())
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void approveAlreadyApprovedOrderReturnsConflict() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();
        UUID orderId = placeOrder(fixture);
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, fixture.venue().manager());
        approveOrder(managerClient, fixture, orderId);
        // Act & Assert
        managerClient.post()
                .uri("/api/v1/venues/{venueId}/orders/{orderId}/approve",
                        fixture.venue().venueId(), orderId)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void closePendingOrderReturnsConflict() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();
        UUID orderId = placeOrder(fixture);
        // Act & Assert
        RestTestClientAuth.withSession(restClient, fixture.venue().manager()).post()
                .uri("/api/v1/venues/{venueId}/orders/{orderId}/close",
                        fixture.venue().venueId(), orderId)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void rejectAlreadyRejectedOrderReturnsConflict() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();
        UUID orderId = placeOrder(fixture);
        // Act & Assert
        RestTestClientAuth.withSession(restClient, fixture.venue().manager()).post()
                .uri("/api/v1/venues/{venueId}/orders/{orderId}/reject",
                        fixture.venue().venueId(), orderId)
                .exchange()
                .expectStatus().isOk();

        RestTestClientAuth.withSession(restClient, fixture.venue().manager()).post()
                .uri("/api/v1/venues/{venueId}/orders/{orderId}/reject",
                        fixture.venue().venueId(), orderId)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void getOrderWithUnknownIdReturnsNotFound() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();
        // Act & Assert
        RestTestClientAuth.withSession(restClient, fixture.venue().manager()).get()
                .uri("/api/v1/venues/{venueId}/orders/{orderId}",
                        fixture.venue().venueId(), UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void listOrdersReturnsEmptyListWhenVenueHasNoOrders() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        // Act
        StaffOrderListApiResponse response = RestTestClientAuth.withSession(restClient, venue.manager()).get()
                .uri("/api/v1/venues/{venueId}/orders", venue.venueId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(StaffOrderListApiResponse.class)
                .returnResult().getResponseBody();
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().data()).isEmpty();
    }

    @Test
    void waiterCanApproveOrder() {
        // Arrange
        OrderTestFixture fixture = orderPolluter.createOrderableTable();
        AuthSession waiter = venuePolluter.addMember(fixture.venue().venueId(), VenueRole.WAITER);
        UUID orderId = placeOrder(fixture);
        // Act
        StaffOrderApiResponse response = RestTestClientAuth.withSession(restClient, waiter).post()
                .uri("/api/v1/venues/{venueId}/orders/{orderId}/approve",
                        fixture.venue().venueId(), orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(StaffOrderApiResponse.class)
                .returnResult().getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().status()).isEqualTo(OrderStatus.APPROVED);
    }

    private UUID placeOrder(OrderTestFixture fixture) {
        OrderApiResponse response = restClient.post()
                .uri("/api/v1/public/tables/{tableId}/orders", fixture.tableId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateOrderRequest(List.of(new CreateOrderRequest.ItemDto(fixture.menuItemId(), 1))))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(OrderApiResponse.class)
                .returnResult().getResponseBody();
        assertThat(response).isNotNull();
        return response.getData().id();
    }

    private void approveOrder(RestTestClient client, OrderTestFixture fixture, UUID orderId) {
        client.post()
                .uri("/api/v1/venues/{venueId}/orders/{orderId}/approve",
                        fixture.venue().venueId(), orderId)
                .exchange()
                .expectStatus().isOk();
    }
}