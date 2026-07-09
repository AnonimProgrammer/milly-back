package com.milly.order.infrastructure.adapter.inbound.http;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.milly.auth.application.port.outbound.WsTicketStore;
import com.milly.auth.domain.model.WsTicket;
import com.milly.common.domain.valueobject.Money;
import com.milly.config.infrastructure.adapter.outbound.websocket.CapturingStompFrameHandler;
import com.milly.config.infrastructure.adapter.outbound.websocket.NoOpStompSessionHandler;
import com.milly.config.domain.constant.StompTopics;
import com.milly.menu.domain.entity.MenuItemEntity;
import com.milly.menu.domain.valueobject.MenuItemStatus;
import com.milly.menu.infrastructure.adapter.outbound.persistence.MenuItemJpaRepository;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.entity.OrderItemEntity;
import com.milly.order.domain.valueobject.OrderStatus;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderWsEventIntegrationTest {

    private static final long TIMEOUT_SECONDS = 5;

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TableJpaRepository tableRepository;

    @Autowired
    private MenuItemJpaRepository menuItemRepository;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private OrderItemJpaRepository orderItemRepository;

    @Autowired
    private VenueMembershipJpaRepository venueMembershipRepository;

    @Autowired
    private WsTicketStore wsTicketStore;

    private UUID venueId;
    private UUID tableId;
    private UUID menuItemId;
    private UUID managerId;

    @BeforeEach
    void setUp() {
        venueId = UUID.randomUUID();
        tableId = tableRepository.save(TableEntity.create(venueId, "Table 1", TableStatus.ACTIVE)).getId();
        menuItemId = menuItemRepository.save(MenuItemEntity.create(
                venueId, "Burger", "Tasty", Money.of("12.50"), 15, MenuItemStatus.ACTIVE)).getId();
        managerId = UUID.randomUUID();
        venueMembershipRepository.save(VenueMembershipEntity.create(venueId, managerId, VenueRole.MANAGER));
    }

    @Test
    void placeOrderPublishesToVenueStaffTopic() throws Exception {
        UUID ticketId = UUID.randomUUID();
        registerTicket(ticketId, managerId);

        StompSession session = connect("?ticket=" + ticketId).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        CapturingStompFrameHandler handler = new CapturingStompFrameHandler(objectMapper);
        session.subscribe(StompTopics.venueStaffTopic(venueId), handler);

        mockMvc.perform(post("/api/v1/public/tables/" + tableId + "/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(placeOrderBody(menuItemId)))
                .andExpect(status().isCreated());

        JsonNode event = handler.awaitPayload(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(event.get("type").asText()).isEqualTo("ORDER_PLACED");
        assertThat(event.get("venueId").asText()).isEqualTo(venueId.toString());
        assertThat(event.get("tableId").asText()).isEqualTo(tableId.toString());
        assertThat(event.get("orderId").asText()).isNotBlank();

        session.disconnect();
    }

    @Test
    void approveOrderPublishesToTableTopic() throws Exception {
        OrderEntity order = orderRepository.save(OrderEntity.create(venueId, tableId, OrderStatus.PENDING));

        StompSession session = connect(null).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        CapturingStompFrameHandler handler = new CapturingStompFrameHandler(objectMapper);
        session.subscribe(StompTopics.tableTopic(tableId), handler);

        mockMvc.perform(post("/api/v1/venues/" + venueId + "/orders/" + order.getId() + "/approve")
                        .with(authentication(new UsernamePasswordAuthenticationToken(managerId, null, List.of()))))
                .andExpect(status().isOk());

        JsonNode event = handler.awaitPayload(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(event.get("type").asText()).isEqualTo("ORDER_APPROVED");
        assertThat(event.get("orderId").asText()).isEqualTo(order.getId().toString());

        session.disconnect();
    }

    @Test
    void processPaymentPublishesToTableAndVenueStaffTopics() throws Exception {
        UUID ticketId = UUID.randomUUID();
        registerTicket(ticketId, managerId);

        OrderEntity order = orderRepository.save(OrderEntity.create(venueId, tableId, OrderStatus.APPROVED));
        orderItemRepository.save(OrderItemEntity.create(order.getId(), menuItemId, 1, Money.of("12.50")));

        StompSession tableSession = connect(null).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        StompSession staffSession = connect("?ticket=" + ticketId).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        CapturingStompFrameHandler tableHandler = new CapturingStompFrameHandler(objectMapper);
        CapturingStompFrameHandler staffHandler = new CapturingStompFrameHandler(objectMapper);
        tableSession.subscribe(StompTopics.tableTopic(tableId), tableHandler);
        staffSession.subscribe(StompTopics.venueStaffTopic(venueId), staffHandler);

        mockMvc.perform(post("/api/v1/public/tables/" + tableId + "/orders/" + order.getId() + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody()))
                .andExpect(status().isCreated());

        JsonNode tableEvent = tableHandler.awaitPayload(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        JsonNode staffEvent = staffHandler.awaitPayload(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertThat(tableEvent.get("type").asText()).isEqualTo("PAYMENT_RECEIVED");
        assertThat(tableEvent.get("orderId").asText()).isEqualTo(order.getId().toString());
        assertThat(staffEvent.get("type").asText()).isEqualTo("PAYMENT_RECEIVED");
        assertThat(staffEvent.get("venueId").asText()).isEqualTo(venueId.toString());

        tableSession.disconnect();
        staffSession.disconnect();
    }

    @Test
    void failedPlaceOrderDoesNotPublish() throws Exception {
        UUID ticketId = UUID.randomUUID();
        registerTicket(ticketId, managerId);

        StompSession session = connect("?ticket=" + ticketId).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        CapturingStompFrameHandler handler = new CapturingStompFrameHandler(objectMapper);
        session.subscribe(StompTopics.venueStaffTopic(venueId), handler);

        mockMvc.perform(post("/api/v1/public/tables/" + tableId + "/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(placeOrderBody(UUID.randomUUID())))
                .andExpect(status().isNotFound());

        assertThatThrownBy(() -> handler.awaitPayload(500, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);

        session.disconnect();
    }

    private String placeOrderBody(UUID itemId) {
        return """
                {"items":[{"menuItemId":"%s","quantity":1}]}
                """.formatted(itemId);
    }

    private String paymentBody() {
        return """
                {
                  "amount": 5.00,
                  "paymentType": "FULL",
                  "provider": "CARD",
                  "providerDetails": {
                    "last4": "4242",
                    "brand": "visa",
                    "expiryMonth": 12,
                    "expiryYear": 2029
                  }
                }
                """;
    }

    private void registerTicket(UUID ticketId, UUID userId) {
        Instant issuedAt = Instant.now();
        wsTicketStore.register(new WsTicket(ticketId, userId, issuedAt, issuedAt.plusSeconds(30)));
    }

    private CompletableFuture<StompSession> connect(String query) {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        String url = "ws://localhost:" + port + "/ws" + (query == null ? "" : query);
        return stompClient.connectAsync(url, new WebSocketHttpHeaders(), new NoOpStompSessionHandler());
    }
}
