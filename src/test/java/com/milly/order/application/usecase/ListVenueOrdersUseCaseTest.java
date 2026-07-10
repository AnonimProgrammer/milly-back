package com.milly.order.application.usecase;

import com.milly.common.domain.valueobject.Money;
import com.milly.common.application.exception.AccessDeniedException;
import com.milly.common.application.dto.PageResponse;
import com.milly.order.application.dto.StaffOrderResponse;
import com.milly.order.application.port.outbound.PaymentSummaryPort;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.entity.OrderItemEntity;
import com.milly.order.domain.valueobject.OrderStatus;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.venue.application.service.VenueAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.milly.order.application.usecase.builder.OrderItemTestBuilder.anOrderItem;
import static com.milly.order.application.usecase.builder.OrderTestBuilder.anOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListVenueOrdersUseCaseTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    @Mock
    private OrderJpaRepository orderRepository;

    @Mock
    private OrderItemJpaRepository orderItemRepository;

    @Mock
    private PaymentSummaryPort paymentSummaryPort;

    private ListVenueOrdersUseCase listVenueOrdersUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listVenueOrdersUseCase = new ListVenueOrdersUseCase(
                venueAuthorizationService, orderRepository, orderItemRepository, paymentSummaryPort);
    }

    @Test
    void returnsAllVenueOrdersWhenStatusFilterIsNull() {
        // Arrange
        OrderEntity pendingOrder = anOrderWithStatus(OrderStatus.PENDING);
        OrderItemEntity lineItem = anOrderItem().withOrderId(orderId).withUnitPrice(Money.of("10.00")).build();
        when(orderRepository.findAllByVenueIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                eq(venueId), any(OffsetDateTime.class), any(OffsetDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pendingOrder), PageRequest.of(0, 1), 2));
        when(orderItemRepository.findAllByOrderIdIn(List.of(orderId))).thenReturn(List.of(lineItem));
        when(paymentSummaryPort.paidAmountsFor(List.of(orderId))).thenReturn(Map.of(orderId, BigDecimal.ZERO));
        when(paymentSummaryPort.tipAmountsFor(List.of(orderId))).thenReturn(Map.of(orderId, BigDecimal.ZERO));

        // Act
        // signature: execute(UUID venueId, UUID userId, OrderStatus status,
        //                    OffsetDateTime from, OffsetDateTime to, String cursor, int limit)
        PageResponse<StaffOrderResponse> response = listVenueOrdersUseCase.execute(venueId, userId, null, null, null, null, 1);

        // Assert
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().id()).isEqualTo(orderId);
        assertThat(response.data().getFirst().items()).hasSize(1);
        assertThat(response.pagination().limit()).isEqualTo(1);
        assertThat(response.pagination().hasNext()).isTrue();
        assertThat(response.pagination().nextCursor()).isEqualTo("1");
        verify(venueAuthorizationService).requireMember(userId, venueId);

        ArgumentCaptor<OffsetDateTime> fromCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> toCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(orderRepository).findAllByVenueIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                eq(venueId), fromCaptor.capture(), toCaptor.capture(), any(Pageable.class));

        ZoneId zone = ZoneId.systemDefault();
        assertThat(fromCaptor.getValue()).isEqualTo(LocalDate.now(zone).atStartOfDay(zone).toOffsetDateTime());
        assertThat(toCaptor.getValue()).isEqualTo(LocalDate.now(zone).atTime(LocalTime.MAX).atZone(zone).toOffsetDateTime());
    }

    @Test
    void filtersOrdersByStatusWhenFilterIsProvided() {
        // Arrange
        OrderEntity approvedOrder = anOrderWithStatus(OrderStatus.APPROVED);
        OffsetDateTime from = OffsetDateTime.parse("2026-07-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-07-31T23:59:59Z");
        when(orderRepository.findAllByVenueIdAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
                eq(venueId), eq(OrderStatus.APPROVED), eq(from), eq(to), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(approvedOrder), PageRequest.of(0, 10), 1));
        when(orderItemRepository.findAllByOrderIdIn(List.of(orderId))).thenReturn(List.of());
        when(paymentSummaryPort.paidAmountsFor(List.of(orderId))).thenReturn(Map.of(orderId, BigDecimal.ZERO));
        when(paymentSummaryPort.tipAmountsFor(List.of(orderId))).thenReturn(Map.of(orderId, BigDecimal.ZERO));

        // Act
        PageResponse<StaffOrderResponse> response = listVenueOrdersUseCase.execute(venueId, userId, OrderStatus.APPROVED, from, to, null, 10);

        // Assert
        assertThat(response.data().getFirst().status()).isEqualTo(OrderStatus.APPROVED);
        verify(venueAuthorizationService).requireMember(userId, venueId);
    }

    @Test
    void throwsAccessDeniedWhenUserIsNotVenueMember() {
        // Arrange
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireMember(userId, venueId);

        // Act & Assert
        assertThatThrownBy(() -> listVenueOrdersUseCase.execute(venueId, userId, null, null, null, null, 20))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(orderRepository, orderItemRepository);
    }

    private OrderEntity anOrderWithStatus(OrderStatus status) {
        return anOrder().withId(orderId).withVenueId(venueId).withTableId(tableId).withStatus(status).build();
    }
}