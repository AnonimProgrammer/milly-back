package com.milly.order.application.usecase;

import com.milly.common.domain.valueobject.Money;
import com.milly.common.exception.AccessDeniedException;
import com.milly.common.exception.ResourceNotFoundException;
import com.milly.order.application.dto.StaffOrderResponse;
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
class StaffOrderQueryUseCasesTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    @Mock
    private OrderJpaRepository orderRepository;

    @Mock
    private OrderItemJpaRepository orderItemRepository;

    private ListVenueOrdersUseCase listVenueOrdersUseCase;
    private GetVenueOrderUseCase getVenueOrderUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listVenueOrdersUseCase = new ListVenueOrdersUseCase(
                venueAuthorizationService, orderRepository, orderItemRepository);
        getVenueOrderUseCase = new GetVenueOrderUseCase(
                venueAuthorizationService, orderRepository, orderItemRepository);
    }

    @Nested
    class ListVenueOrders {

        @Test
        void returnsAllVenueOrdersWhenStatusFilterIsNull() {
            OrderEntity pendingOrder = anOrderWithStatus(OrderStatus.PENDING);
            OrderItemEntity lineItem = anOrderItem().withOrderId(orderId).withUnitPrice(Money.of("10.00")).build();
            when(orderRepository.findAllByVenueIdOrderByCreatedAtDesc(venueId)).thenReturn(List.of(pendingOrder));
            when(orderItemRepository.findAllByOrderIdIn(List.of(orderId))).thenReturn(List.of(lineItem));

            List<StaffOrderResponse> response = listVenueOrdersUseCase.execute(venueId, userId, null);

            assertThat(response).hasSize(1);
            assertThat(response.getFirst().id()).isEqualTo(orderId);
            assertThat(response.getFirst().items()).hasSize(1);
            verify(venueAuthorizationService).requireMember(userId, venueId);
        }

        @Test
        void filtersOrdersByStatusWhenFilterIsProvided() {
            OrderEntity approvedOrder = anOrderWithStatus(OrderStatus.APPROVED);
            when(orderRepository.findAllByVenueIdAndStatusOrderByCreatedAtDesc(venueId, OrderStatus.APPROVED))
                    .thenReturn(List.of(approvedOrder));
            when(orderItemRepository.findAllByOrderIdIn(List.of(orderId))).thenReturn(List.of());

            List<StaffOrderResponse> response = listVenueOrdersUseCase.execute(venueId, userId, OrderStatus.APPROVED);

            assertThat(response.getFirst().status()).isEqualTo(OrderStatus.APPROVED);
            verify(venueAuthorizationService).requireMember(userId, venueId);
        }

        @Test
        void throwsAccessDeniedWhenUserIsNotVenueMember() {
            doThrow(new AccessDeniedException())
                    .when(venueAuthorizationService).requireMember(userId, venueId);

            assertThatThrownBy(() -> listVenueOrdersUseCase.execute(venueId, userId, null))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(orderRepository, orderItemRepository);
        }
    }

    @Nested
    class GetVenueOrder {

        @Test
        void returnsOrderScopedToVenue() {
            OrderEntity pendingOrder = anOrderWithStatus(OrderStatus.PENDING);
            OrderItemEntity lineItem = anOrderItem().withOrderId(orderId).withUnitPrice(Money.of("10.00")).build();
            when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.of(pendingOrder));
            when(orderItemRepository.findAllByOrderId(orderId)).thenReturn(List.of(lineItem));

            StaffOrderResponse response = getVenueOrderUseCase.execute(venueId, userId, orderId);

            assertThat(response.id()).isEqualTo(orderId);
            assertThat(response.tableId()).isEqualTo(tableId);
            verify(venueAuthorizationService).requireMember(userId, venueId);
        }

        @Test
        void throwsNotFoundWhenOrderBelongsToDifferentVenue() {
            when(orderRepository.findByIdAndVenueId(orderId, venueId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> getVenueOrderUseCase.execute(venueId, userId, orderId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void throwsAccessDeniedWhenUserIsNotVenueMember() {
            doThrow(new AccessDeniedException())
                    .when(venueAuthorizationService).requireMember(userId, venueId);

            assertThatThrownBy(() -> getVenueOrderUseCase.execute(venueId, userId, orderId))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(orderRepository, orderItemRepository);
        }
    }

    private OrderEntity anOrderWithStatus(OrderStatus status) {
        return anOrder().withId(orderId).withVenueId(venueId).withTableId(tableId).withStatus(status).build();
    }
}
