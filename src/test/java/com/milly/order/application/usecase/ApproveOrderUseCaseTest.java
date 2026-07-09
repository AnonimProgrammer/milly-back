package com.milly.order.application.usecase;

import com.milly.common.domain.valueobject.Money;
import com.milly.common.exception.AccessDeniedException;
import com.milly.common.exception.InvalidStateTransitionException;
import com.milly.common.exception.ResourceNotFoundException;
import com.milly.order.application.dto.StaffOrderResponse;
import com.milly.order.application.service.OrderEventNotifier;
import com.milly.order.application.service.OrderPreparationEstimator;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.entity.OrderItemEntity;
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

import static com.milly.order.application.usecase.builder.OrderItemTestBuilder.anOrderItem;
import static com.milly.order.application.usecase.builder.OrderTestBuilder.anOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApproveOrderUseCaseTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    @Mock
    private OrderJpaRepository orderRepository;

    @Mock
    private OrderItemJpaRepository orderItemRepository;

    @Mock
    private OrderEventNotifier orderEventNotifier;

    @Mock
    private OrderPreparationEstimator orderPreparationEstimator;

    private ApproveOrderUseCase approveOrderUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        approveOrderUseCase = new ApproveOrderUseCase(
                venueAuthorizationService,
                orderRepository,
                orderItemRepository,
                orderEventNotifier,
                orderPreparationEstimator);
    }

    @Test
    void transitionsPendingOrderToApproved() {
        // Arrange
        OrderEntity pendingOrder = anOrderWithStatus(OrderStatus.PENDING);
        when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.of(pendingOrder));
        when(orderItemRepository.findAllByOrderId(orderId)).thenReturn(List.of());

        // Act
        StaffOrderResponse response = approveOrderUseCase.execute(venueId, userId, orderId);

        // Assert
        assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.APPROVED);
        assertThat(pendingOrder.getApprovedAt()).isNotNull();
        assertThat(response.status()).isEqualTo(OrderStatus.APPROVED);
        verify(venueAuthorizationService).requireMember(userId, venueId);
        verify(orderPreparationEstimator).tryEstimate(venueId, orderId, List.of());
        verify(orderEventNotifier).orderApproved(orderId, venueId, tableId);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"APPROVED", "REJECTED", "CLOSED"})
    void throwsInvalidTransitionWhenOrderIsNotPending(OrderStatus status) {
        // Arrange
        OrderEntity order = anOrderWithStatus(status);
        when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThatThrownBy(() -> approveOrderUseCase.execute(venueId, userId, orderId))
                .isInstanceOf(InvalidStateTransitionException.class);

        verifyNoInteractions(orderItemRepository, orderEventNotifier, orderPreparationEstimator);
    }

    @Test
    void throwsNotFoundWhenOrderBelongsToDifferentVenue() {
        // Arrange
        when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> approveOrderUseCase.execute(venueId, userId, orderId))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(orderItemRepository, orderEventNotifier, orderPreparationEstimator);
    }

    @Test
    void throwsAccessDeniedWhenUserIsNotVenueMember() {
        // Arrange
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireMember(userId, venueId);

        // Act & Assert
        assertThatThrownBy(() -> approveOrderUseCase.execute(venueId, userId, orderId))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(orderRepository, orderEventNotifier);
    }

    @Test
    void returnsResponseWithOrderItems() {
        // Arrange
        OrderEntity pendingOrder = anOrderWithStatus(OrderStatus.PENDING);
        OrderItemEntity lineItem = anOrderItem().withOrderId(orderId).withQuantity(2)
                .withUnitPrice(Money.of("15.00")).build();
        when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.of(pendingOrder));
        when(orderItemRepository.findAllByOrderId(orderId)).thenReturn(List.of(lineItem));

        // Act
        StaffOrderResponse response = approveOrderUseCase.execute(venueId, userId, orderId);

        // Assert
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().quantity()).isEqualTo(2);
    }

    private OrderEntity anOrderWithStatus(OrderStatus status) {
        return anOrder().withId(orderId).withVenueId(venueId).withTableId(tableId).withStatus(status).build();
    }
}
