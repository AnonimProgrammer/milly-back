package com.milly.order.application.usecase;

import com.milly.common.domain.valueobject.Money;
import com.milly.common.application.exception.ResourceNotFoundException;
import com.milly.order.application.dto.OrderResponse;
import com.milly.order.application.port.outbound.PaymentSummaryPort;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.entity.OrderItemEntity;
import com.milly.order.domain.valueobject.OrderStatus;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.milly.order.application.usecase.builder.OrderItemTestBuilder.anOrderItem;
import static com.milly.order.application.usecase.builder.OrderTestBuilder.anOrder;
import static com.milly.table.application.usecase.builder.TableTestBuilder.aTable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListOrdersUseCaseTest {

    @Mock
    private TableJpaRepository tableRepository;

    @Mock
    private OrderJpaRepository orderRepository;

    @Mock
    private OrderItemJpaRepository orderItemRepository;

    @Mock
    private PaymentSummaryPort paymentSummaryPort;

    private ListOrdersUseCase listOrdersUseCase;

    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID menuItemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listOrdersUseCase = new ListOrdersUseCase(
                tableRepository, orderRepository, orderItemRepository, paymentSummaryPort);
    }

    @Test
    void returnsOrdersWithGroupedItemsForActiveTable() {
        // Arrange
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(anActiveTable()));
        OrderEntity pendingOrder = anOrder().withId(orderId).withVenueId(venueId).withTableId(tableId)
                .withStatus(OrderStatus.PENDING).build();
        OrderItemEntity lineItem = anOrderItem().withOrderId(orderId).withMenuItemId(menuItemId)
                .withUnitPrice(Money.of("9.99")).build();
        when(orderRepository.findAllByTableIdOrderByCreatedAtDesc(tableId)).thenReturn(List.of(pendingOrder));
        when(orderItemRepository.findAllByOrderIdIn(List.of(orderId))).thenReturn(List.of(lineItem));
        when(paymentSummaryPort.paidAmountsFor(List.of(orderId))).thenReturn(Map.of(orderId, BigDecimal.ZERO));

        // Act
        List<OrderResponse> response = listOrdersUseCase.execute(tableId);

        // Assert
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().id()).isEqualTo(orderId);
        assertThat(response.getFirst().items().getFirst().menuItemId()).isEqualTo(menuItemId);
    }

    @Test
    void returnsEmptyListWhenTableHasNoOrders() {
        // Arrange
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(anActiveTable()));
        when(orderRepository.findAllByTableIdOrderByCreatedAtDesc(tableId)).thenReturn(List.of());
        when(orderItemRepository.findAllByOrderIdIn(List.of())).thenReturn(List.of());
        when(paymentSummaryPort.paidAmountsFor(List.of())).thenReturn(Map.of());

        // Act
        List<OrderResponse> response = listOrdersUseCase.execute(tableId);

        // Assert
        assertThat(response).isEmpty();
    }

    @Test
    void throwsNotFoundWhenTableIsMissing() {
        // Arrange
        when(tableRepository.findById(tableId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> listOrdersUseCase.execute(tableId))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(orderRepository, orderItemRepository);
    }

    private TableEntity anActiveTable() {
        return aTable().withId(tableId).withVenueId(venueId).build();
    }
}