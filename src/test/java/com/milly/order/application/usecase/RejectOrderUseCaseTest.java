package com.milly.order.application.usecase;

import com.milly.common.application.exception.AccessDeniedException;
import com.milly.common.application.exception.InvalidStateTransitionException;
import com.milly.common.application.exception.ResourceNotFoundException;
import com.milly.order.application.dto.StaffOrderResponse;
import com.milly.order.application.service.OrderEventNotifier;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.valueobject.OrderStatus;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.venue.application.service.VenueAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.milly.order.application.usecase.builder.OrderTestBuilder.anOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RejectOrderUseCaseTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    @Mock
    private OrderJpaRepository orderRepository;

    @Mock
    private OrderItemJpaRepository orderItemRepository;

    @Mock
    private OrderEventNotifier orderEventNotifier;

    private RejectOrderUseCase rejectOrderUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        rejectOrderUseCase = new RejectOrderUseCase(
                venueAuthorizationService, orderRepository, orderItemRepository, orderEventNotifier);
    }

    @Test
    void transitionsPendingOrderToRejected() {
        // Arrange
        OrderEntity pendingOrder = anOrderWithStatus(OrderStatus.PENDING);
        when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.of(pendingOrder));
        when(orderItemRepository.findAllByOrderId(orderId)).thenReturn(List.of());

        // Act
        StaffOrderResponse response = rejectOrderUseCase.execute(venueId, userId, orderId);

        // Assert
        assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(response.status()).isEqualTo(OrderStatus.REJECTED);
        verify(venueAuthorizationService).requireActiveMember(userId, venueId);
        verify(orderEventNotifier).orderRejected(orderId, venueId, tableId);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"APPROVED", "REJECTED", "CLOSED"})
    void throwsInvalidTransitionWhenOrderIsNotPending(OrderStatus status) {
        // Arrange
        OrderEntity order = anOrderWithStatus(status);
        when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThatThrownBy(() -> rejectOrderUseCase.execute(venueId, userId, orderId))
                .isInstanceOf(InvalidStateTransitionException.class);

        verifyNoInteractions(orderItemRepository, orderEventNotifier);
    }

    @Test
    void throwsNotFoundWhenOrderBelongsToDifferentVenue() {
        // Arrange
        when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> rejectOrderUseCase.execute(venueId, userId, orderId))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(orderItemRepository, orderEventNotifier);
    }

    @Test
    void throwsAccessDeniedWhenUserIsNotVenueMember() {
        // Arrange
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireActiveMember(userId, venueId);

        // Act & Assert
        assertThatThrownBy(() -> rejectOrderUseCase.execute(venueId, userId, orderId))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(orderRepository, orderEventNotifier);
    }

    private OrderEntity anOrderWithStatus(OrderStatus status) {
        return anOrder().withId(orderId).withVenueId(venueId).withTableId(tableId).withStatus(status).build();
    }
}
