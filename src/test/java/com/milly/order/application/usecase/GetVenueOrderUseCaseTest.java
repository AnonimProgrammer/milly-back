package com.milly.order.application.usecase;

import com.milly.common.domain.valueobject.Money;
import com.milly.common.application.exception.AccessDeniedException;
import com.milly.common.application.exception.ResourceNotFoundException;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.milly.order.application.usecase.builder.OrderItemTestBuilder.anOrderItem;
import static com.milly.order.application.usecase.builder.OrderTestBuilder.anOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetVenueOrderUseCaseTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    @Mock
    private OrderJpaRepository orderRepository;

    @Mock
    private OrderItemJpaRepository orderItemRepository;

    @Mock
    private PaymentSummaryPort paymentSummaryPort;

    private GetVenueOrderUseCase getVenueOrderUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        getVenueOrderUseCase = new GetVenueOrderUseCase(
                venueAuthorizationService, orderRepository, orderItemRepository, paymentSummaryPort);
    }

    @Test
    void returnsOrderScopedToVenue() {
        // Arrange
        OrderEntity pendingOrder = anOrderWithStatus(OrderStatus.PENDING);
        OrderItemEntity lineItem = anOrderItem().withOrderId(orderId).withUnitPrice(Money.of("10.00")).build();
        when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.of(pendingOrder));
        when(orderItemRepository.findAllByOrderId(orderId)).thenReturn(List.of(lineItem));
        when(paymentSummaryPort.paidAmountFor(orderId)).thenReturn(BigDecimal.ZERO);
        when(paymentSummaryPort.tipAmountFor(orderId)).thenReturn(BigDecimal.ZERO);

        // Act
        StaffOrderResponse response = getVenueOrderUseCase.execute(venueId, userId, orderId);

        // Assert
        assertThat(response.id()).isEqualTo(orderId);
        assertThat(response.tableId()).isEqualTo(tableId);
        verify(venueAuthorizationService).requireMember(userId, venueId);
    }

    @Test
    void throwsNotFoundWhenOrderBelongsToDifferentVenue() {
        // Arrange
        when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> getVenueOrderUseCase.execute(venueId, userId, orderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void throwsAccessDeniedWhenUserIsNotVenueMember() {
        // Arrange
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireMember(userId, venueId);

        // Act & Assert
        assertThatThrownBy(() -> getVenueOrderUseCase.execute(venueId, userId, orderId))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(orderRepository, orderItemRepository);
    }

    private OrderEntity anOrderWithStatus(OrderStatus status) {
        return anOrder().withId(orderId).withVenueId(venueId).withTableId(tableId).withStatus(status).build();
    }
}