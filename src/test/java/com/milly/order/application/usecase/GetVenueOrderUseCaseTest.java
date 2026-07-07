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
class GetVenueOrderUseCaseTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    @Mock
    private OrderJpaRepository orderRepository;

    @Mock
    private OrderItemJpaRepository orderItemRepository;

    private GetVenueOrderUseCase getVenueOrderUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        getVenueOrderUseCase = new GetVenueOrderUseCase(
                venueAuthorizationService, orderRepository, orderItemRepository);
    }

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

    private OrderEntity anOrderWithStatus(OrderStatus status) {
        return anOrder().withId(orderId).withVenueId(venueId).withTableId(tableId).withStatus(status).build();
    }
}
