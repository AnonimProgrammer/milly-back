package com.milly.order.application.usecase;

import com.milly.common.domain.valueobject.Money;
import com.milly.common.exception.AccessDeniedException;
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
class ListVenueOrdersUseCaseTest {

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    @Mock
    private OrderJpaRepository orderRepository;

    @Mock
    private OrderItemJpaRepository orderItemRepository;

    private ListVenueOrdersUseCase listVenueOrdersUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listVenueOrdersUseCase = new ListVenueOrdersUseCase(
                venueAuthorizationService, orderRepository, orderItemRepository);
    }

    @Test
    void returnsAllVenueOrdersWhenStatusFilterIsNull() {
        // Arrange
        OrderEntity pendingOrder = anOrderWithStatus(OrderStatus.PENDING);
        OrderItemEntity lineItem = anOrderItem().withOrderId(orderId).withUnitPrice(Money.of("10.00")).build();
        when(orderRepository.findAllByVenueIdOrderByCreatedAtDesc(venueId)).thenReturn(List.of(pendingOrder));
        when(orderItemRepository.findAllByOrderIdIn(List.of(orderId))).thenReturn(List.of(lineItem));

        // Act
        List<StaffOrderResponse> response = listVenueOrdersUseCase.execute(venueId, userId, null);

        // Assert
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().id()).isEqualTo(orderId);
        assertThat(response.getFirst().items()).hasSize(1);
        verify(venueAuthorizationService).requireMember(userId, venueId);
    }

    @Test
    void filtersOrdersByStatusWhenFilterIsProvided() {
        // Arrange
        OrderEntity approvedOrder = anOrderWithStatus(OrderStatus.APPROVED);
        when(orderRepository.findAllByVenueIdAndStatusOrderByCreatedAtDesc(venueId, OrderStatus.APPROVED))
                .thenReturn(List.of(approvedOrder));
        when(orderItemRepository.findAllByOrderIdIn(List.of(orderId))).thenReturn(List.of());

        // Act
        List<StaffOrderResponse> response = listVenueOrdersUseCase.execute(venueId, userId, OrderStatus.APPROVED);

        // Assert
        assertThat(response.getFirst().status()).isEqualTo(OrderStatus.APPROVED);
        verify(venueAuthorizationService).requireMember(userId, venueId);
    }

    @Test
    void throwsAccessDeniedWhenUserIsNotVenueMember() {
        // Arrange
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireMember(userId, venueId);

        // Act & Assert
        assertThatThrownBy(() -> listVenueOrdersUseCase.execute(venueId, userId, null))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(orderRepository, orderItemRepository);
    }

    private OrderEntity anOrderWithStatus(OrderStatus status) {
        return anOrder().withId(orderId).withVenueId(venueId).withTableId(tableId).withStatus(status).build();
    }
}
