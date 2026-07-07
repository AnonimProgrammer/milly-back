package com.milly.order.application.usecase;

import com.milly.common.domain.valueobject.Money;
import com.milly.common.exception.AccessDeniedException;
import com.milly.common.exception.InvalidStateTransitionException;
import com.milly.common.exception.ResourceNotFoundException;
import com.milly.order.application.dto.StaffOrderResponse;
import com.milly.order.application.service.OrderEventNotifier;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.entity.OrderItemEntity;
import com.milly.order.domain.valueobject.OrderStatus;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.venue.application.service.VenueAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.milly.order.application.usecase.OrderItemTestBuilder.anOrderItem;
import static com.milly.order.application.usecase.OrderTestBuilder.anOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffOrderMutationUseCasesTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    @Mock
    private OrderJpaRepository orderRepository;

    @Mock
    private OrderItemJpaRepository orderItemRepository;

    @Mock
    private OrderEventNotifier orderEventNotifier;

    private ApproveOrderUseCase approveOrderUseCase;
    private RejectOrderUseCase rejectOrderUseCase;
    private CloseOrderUseCase closeOrderUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        approveOrderUseCase = new ApproveOrderUseCase(
                venueAuthorizationService, orderRepository, orderItemRepository, orderEventNotifier);
        rejectOrderUseCase = new RejectOrderUseCase(
                venueAuthorizationService, orderRepository, orderItemRepository, orderEventNotifier);
        closeOrderUseCase = new CloseOrderUseCase(
                venueAuthorizationService, orderRepository, orderItemRepository, orderEventNotifier);
    }

    @Nested
    class ApproveOrder {

        @Test
        void transitionsPendingOrderToApproved() {
            OrderEntity pendingOrder = anOrderWithStatus(OrderStatus.PENDING);
            when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.of(pendingOrder));
            when(orderItemRepository.findAllByOrderId(orderId)).thenReturn(List.of());

            StaffOrderResponse response = approveOrderUseCase.execute(venueId, userId, orderId);

            assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.APPROVED);
            assertThat(response.status()).isEqualTo(OrderStatus.APPROVED);
            verify(venueAuthorizationService).requireMember(userId, venueId);
            verify(orderEventNotifier).orderApproved(orderId, venueId, tableId);
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"APPROVED", "REJECTED", "CLOSED"})
        void throwsInvalidTransitionWhenOrderIsNotPending(OrderStatus status) {
            OrderEntity order = anOrderWithStatus(status);
            when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> approveOrderUseCase.execute(venueId, userId, orderId))
                    .isInstanceOf(InvalidStateTransitionException.class);

            verifyNoInteractions(orderItemRepository, orderEventNotifier);
        }

        @Test
        void throwsNotFoundWhenOrderBelongsToDifferentVenue() {
            when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> approveOrderUseCase.execute(venueId, userId, orderId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(orderItemRepository, orderEventNotifier);
        }

        @Test
        void throwsAccessDeniedWhenUserIsNotVenueMember() {
            denyMember();

            assertThatThrownBy(() -> approveOrderUseCase.execute(venueId, userId, orderId))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(orderRepository, orderEventNotifier);
        }

        @Test
        void returnsResponseWithOrderItems() {
            OrderEntity pendingOrder = anOrderWithStatus(OrderStatus.PENDING);
            OrderItemEntity lineItem = anOrderItem().withOrderId(orderId).withQuantity(2)
                    .withUnitPrice(Money.of("15.00")).build();
            when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.of(pendingOrder));
            when(orderItemRepository.findAllByOrderId(orderId)).thenReturn(List.of(lineItem));

            StaffOrderResponse response = approveOrderUseCase.execute(venueId, userId, orderId);

            assertThat(response.items()).hasSize(1);
            assertThat(response.items().getFirst().quantity()).isEqualTo(2);
        }
    }

    @Nested
    class RejectOrder {

        @Test
        void transitionsPendingOrderToRejected() {
            OrderEntity pendingOrder = anOrderWithStatus(OrderStatus.PENDING);
            when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.of(pendingOrder));
            when(orderItemRepository.findAllByOrderId(orderId)).thenReturn(List.of());

            StaffOrderResponse response = rejectOrderUseCase.execute(venueId, userId, orderId);

            assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.REJECTED);
            assertThat(response.status()).isEqualTo(OrderStatus.REJECTED);
            verify(venueAuthorizationService).requireMember(userId, venueId);
            verify(orderEventNotifier).orderRejected(orderId, venueId, tableId);
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"APPROVED", "REJECTED", "CLOSED"})
        void throwsInvalidTransitionWhenOrderIsNotPending(OrderStatus status) {
            OrderEntity order = anOrderWithStatus(status);
            when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> rejectOrderUseCase.execute(venueId, userId, orderId))
                    .isInstanceOf(InvalidStateTransitionException.class);

            verifyNoInteractions(orderItemRepository, orderEventNotifier);
        }

        @Test
        void throwsNotFoundWhenOrderBelongsToDifferentVenue() {
            when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> rejectOrderUseCase.execute(venueId, userId, orderId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(orderItemRepository, orderEventNotifier);
        }

        @Test
        void throwsAccessDeniedWhenUserIsNotVenueMember() {
            denyMember();

            assertThatThrownBy(() -> rejectOrderUseCase.execute(venueId, userId, orderId))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(orderRepository, orderEventNotifier);
        }
    }

    @Nested
    class CloseOrder {

        @Test
        void transitionsApprovedOrderToClosedWithTimestamp() {
            OrderEntity approvedOrder = anOrderWithStatus(OrderStatus.APPROVED);
            when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.of(approvedOrder));
            when(orderItemRepository.findAllByOrderId(orderId)).thenReturn(List.of());

            StaffOrderResponse response = closeOrderUseCase.execute(venueId, userId, orderId);

            assertThat(approvedOrder.getStatus()).isEqualTo(OrderStatus.CLOSED);
            assertThat(approvedOrder.getClosedAt()).isNotNull();
            assertThat(response.status()).isEqualTo(OrderStatus.CLOSED);
            assertThat(response.closedAt()).isNotNull();
            verify(venueAuthorizationService).requireMember(userId, venueId);
            verify(orderEventNotifier).orderClosed(orderId, venueId, tableId);
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"PENDING", "REJECTED", "CLOSED"})
        void throwsInvalidTransitionWhenOrderIsNotApproved(OrderStatus status) {
            OrderEntity order = anOrderWithStatus(status);
            when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> closeOrderUseCase.execute(venueId, userId, orderId))
                    .isInstanceOf(InvalidStateTransitionException.class);

            verifyNoInteractions(orderItemRepository, orderEventNotifier);
        }

        @Test
        void throwsNotFoundWhenOrderBelongsToDifferentVenue() {
            when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> closeOrderUseCase.execute(venueId, userId, orderId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(orderItemRepository, orderEventNotifier);
        }

        @Test
        void throwsAccessDeniedWhenUserIsNotVenueMember() {
            denyMember();

            assertThatThrownBy(() -> closeOrderUseCase.execute(venueId, userId, orderId))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(orderRepository, orderEventNotifier);
        }
    }

    private void denyMember() {
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireMember(userId, venueId);
    }

    private OrderEntity anOrderWithStatus(OrderStatus status) {
        return anOrder().withId(orderId).withVenueId(venueId).withTableId(tableId).withStatus(status).build();
    }
}
