package com.milly.order.application.usecase;

import com.milly.common.domain.valueobject.Money;
import com.milly.common.exception.ResourceNotFoundException;
import com.milly.order.application.dto.OrderResponse;
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
import java.util.Optional;
import java.util.UUID;

import static com.milly.order.application.usecase.OrderItemTestBuilder.anOrderItem;
import static com.milly.order.application.usecase.OrderTestBuilder.anOrder;
import static com.milly.order.application.usecase.TableTestBuilder.aTable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOrderUseCaseTest {

    @Mock
    private TableJpaRepository tableRepository;

    @Mock
    private OrderJpaRepository orderRepository;

    @Mock
    private OrderItemJpaRepository orderItemRepository;

    private GetOrderUseCase getOrderUseCase;

    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID menuItemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        getOrderUseCase = new GetOrderUseCase(tableRepository, orderRepository, orderItemRepository);
    }

    @Test
    void returnsOrderScopedToTable() {
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(anActiveTable()));
        OrderEntity approvedOrder = anOrder().withId(orderId).withVenueId(venueId).withTableId(tableId)
                .withStatus(OrderStatus.APPROVED).build();
        OrderItemEntity lineItem = anOrderItem().withOrderId(orderId).withMenuItemId(menuItemId)
                .withQuantity(3).withUnitPrice(Money.of("4.00")).build();
        when(orderRepository.findByIdAndTableId(orderId, tableId)).thenReturn(Optional.of(approvedOrder));
        when(orderItemRepository.findAllByOrderId(orderId)).thenReturn(List.of(lineItem));

        OrderResponse response = getOrderUseCase.execute(tableId, orderId);

        assertThat(response.id()).isEqualTo(orderId);
        assertThat(response.status()).isEqualTo(OrderStatus.APPROVED);
        assertThat(response.items().getFirst().unitPrice()).isEqualByComparingTo(new BigDecimal("4.00"));
    }

    @Test
    void throwsNotFoundWhenTableIsMissing() {
        when(tableRepository.findById(tableId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getOrderUseCase.execute(tableId, orderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenOrderDoesNotBelongToTable() {
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(anActiveTable()));
        when(orderRepository.findByIdAndTableId(orderId, tableId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getOrderUseCase.execute(tableId, orderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private TableEntity anActiveTable() {
        return aTable().withId(tableId).withVenueId(venueId).build();
    }
}
