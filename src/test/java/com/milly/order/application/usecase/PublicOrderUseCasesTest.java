package com.milly.order.application.usecase;

import com.milly.common.domain.valueobject.Money;
import com.milly.common.exception.InvalidStateTransitionException;
import com.milly.common.exception.ResourceNotFoundException;
import com.milly.menu.domain.entity.MenuItemEntity;
import com.milly.menu.domain.valueobject.MenuItemStatus;
import com.milly.menu.infrastructure.adapter.outbound.persistence.MenuItemJpaRepository;
import com.milly.order.application.dto.AddOrderItemsRequest;
import com.milly.order.application.dto.CreateOrderRequest;
import com.milly.order.application.dto.OrderResponse;
import com.milly.order.application.service.OrderEventNotifier;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.entity.OrderItemEntity;
import com.milly.order.domain.valueobject.OrderStatus;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.milly.order.application.usecase.MenuItemTestBuilder.aMenuItem;
import static com.milly.order.application.usecase.OrderItemTestBuilder.anOrderItem;
import static com.milly.order.application.usecase.OrderTestBuilder.anOrder;
import static com.milly.order.application.usecase.TableTestBuilder.aTable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicOrderUseCasesTest {

    @Mock
    private TableJpaRepository tableRepository;

    @Mock
    private MenuItemJpaRepository menuItemRepository;

    @Mock
    private OrderJpaRepository orderRepository;

    @Mock
    private OrderItemJpaRepository orderItemRepository;

    @Mock
    private OrderEventNotifier orderEventNotifier;

    private GetOrderUseCase getOrderUseCase;
    private AddOrderItemsUseCase addOrderItemsUseCase;

    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID menuItemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        getOrderUseCase = new GetOrderUseCase(tableRepository, orderRepository, orderItemRepository);
        addOrderItemsUseCase = new AddOrderItemsUseCase(
                tableRepository, menuItemRepository, orderRepository, orderItemRepository, orderEventNotifier);
    }

    @Nested
    class GetOrder {

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
    }

    @Nested
    class AddOrderItems {

        @Test
        void appendsItemsToApprovedOrderWithSnapshottedPrice() {
            TableEntity table = anActiveTable();
            OrderEntity approvedOrder = anOrder().withId(orderId).withVenueId(venueId).withTableId(tableId)
                    .withStatus(OrderStatus.APPROVED).build();
            MenuItemEntity burger = aMenuItem().withId(menuItemId).withVenueId(venueId)
                    .withPrice(Money.of("7.25")).build();
            OrderItemEntity existingItem = anOrderItem().withOrderId(orderId)
                    .withUnitPrice(Money.of("3.00")).build();
            OrderItemEntity newItem = anOrderItem().withOrderId(orderId).withMenuItemId(menuItemId)
                    .withQuantity(2).withUnitPrice(Money.of("7.25")).build();
            when(tableRepository.findById(tableId)).thenReturn(Optional.of(table));
            when(orderRepository.findByIdAndTableId(orderId, tableId)).thenReturn(Optional.of(approvedOrder));
            when(menuItemRepository.findAllById(List.of(menuItemId))).thenReturn(List.of(burger));
            when(orderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderItemRepository.findAllByOrderId(orderId)).thenReturn(List.of(existingItem, newItem));

            OrderResponse response = addOrderItemsUseCase.execute(
                    tableId, orderId, new AddOrderItemsRequest(List.of(new CreateOrderRequest.ItemDto(menuItemId, 2))));

            assertThat(response.items()).hasSize(2);
            assertThat(response.items().get(1).unitPrice()).isEqualByComparingTo(new BigDecimal("7.25"));
            ArgumentCaptor<List<OrderItemEntity>> captor = ArgumentCaptor.forClass(List.class);
            verify(orderItemRepository).saveAll(captor.capture());
            assertThat(captor.getValue())
                    .extracting(OrderItemEntity::getMenuItemId)
                    .contains(menuItemId);
            verify(orderEventNotifier).orderUpdated(orderId, venueId, tableId);
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"PENDING", "REJECTED", "CLOSED"})
        void throwsInvalidTransitionWhenOrderIsNotApproved(OrderStatus status) {
            when(tableRepository.findById(tableId)).thenReturn(Optional.of(anActiveTable()));
            when(orderRepository.findByIdAndTableId(orderId, tableId))
                    .thenReturn(Optional.of(anOrder().withId(orderId).withVenueId(venueId).withTableId(tableId)
                            .withStatus(status).build()));

            assertThatThrownBy(() -> addOrderItemsUseCase.execute(
                    tableId, orderId, new AddOrderItemsRequest(List.of(new CreateOrderRequest.ItemDto(menuItemId, 1)))))
                    .isInstanceOf(InvalidStateTransitionException.class);

            verifyNoInteractions(menuItemRepository, orderItemRepository, orderEventNotifier);
        }

        @Test
        void throwsNotFoundWhenOrderDoesNotBelongToTable() {
            when(tableRepository.findById(tableId)).thenReturn(Optional.of(anActiveTable()));
            when(orderRepository.findByIdAndTableId(orderId, tableId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> addOrderItemsUseCase.execute(
                    tableId, orderId, new AddOrderItemsRequest(List.of(new CreateOrderRequest.ItemDto(menuItemId, 1)))))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void throwsNotFoundWhenMenuItemIsInvalid() {
            OrderEntity approvedOrder = anOrder().withId(orderId).withVenueId(venueId).withTableId(tableId)
                    .withStatus(OrderStatus.APPROVED).build();
            when(tableRepository.findById(tableId)).thenReturn(Optional.of(anActiveTable()));
            when(orderRepository.findByIdAndTableId(orderId, tableId)).thenReturn(Optional.of(approvedOrder));
            when(menuItemRepository.findAllById(List.of(menuItemId))).thenReturn(List.of());

            assertThatThrownBy(() -> addOrderItemsUseCase.execute(
                    tableId, orderId, new AddOrderItemsRequest(List.of(new CreateOrderRequest.ItemDto(menuItemId, 1)))))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    private TableEntity anActiveTable() {
        return aTable().withId(tableId).withVenueId(venueId).build();
    }
}
