package com.milly.order.application.usecase;

import com.milly.common.domain.valueobject.Money;
import com.milly.common.exception.ResourceNotFoundException;
import com.milly.menu.domain.entity.MenuItemEntity;
import com.milly.menu.domain.valueobject.MenuItemStatus;
import com.milly.menu.infrastructure.adapter.outbound.persistence.MenuItemJpaRepository;
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

    private CreateOrderUseCase createOrderUseCase;
    private ListOrdersUseCase listOrdersUseCase;

    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID menuItemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        createOrderUseCase = new CreateOrderUseCase(
                tableRepository, menuItemRepository, orderRepository, orderItemRepository, orderEventNotifier);
        listOrdersUseCase = new ListOrdersUseCase(tableRepository, orderRepository, orderItemRepository);
    }

    @Nested
    class CreateOrder {

        @Test
        void persistsPendingOrderWithSnapshottedPrices() {
            TableEntity table = aTable().withId(tableId).withVenueId(venueId).build();
            MenuItemEntity burger = aMenuItem().withId(menuItemId).withVenueId(venueId).withPrice(Money.of("12.50")).build();
            when(tableRepository.findById(tableId)).thenReturn(Optional.of(table));
            when(menuItemRepository.findAllById(List.of(menuItemId))).thenReturn(List.of(burger));
            when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
                OrderEntity savedOrder = invocation.getArgument(0);
                savedOrder.setId(orderId);
                return savedOrder;
            });
            when(orderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

            OrderResponse response = createOrderUseCase.execute(
                    tableId, new CreateOrderRequest(List.of(new CreateOrderRequest.ItemDto(menuItemId, 2))));

            assertThat(response.id()).isEqualTo(orderId);
            assertThat(response.tableId()).isEqualTo(tableId);
            assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().getFirst().menuItemId()).isEqualTo(menuItemId);
            assertThat(response.items().getFirst().quantity()).isEqualTo(2);
            assertThat(response.items().getFirst().unitPrice()).isEqualByComparingTo(new BigDecimal("12.50"));
            verify(orderEventNotifier).orderPlaced(orderId, venueId, tableId);
        }

        @Test
        void throwsNotFoundWhenTableIsMissing() {
            when(tableRepository.findById(tableId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> createOrderUseCase.execute(
                    tableId, new CreateOrderRequest(List.of(new CreateOrderRequest.ItemDto(menuItemId, 1)))))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(orderRepository, menuItemRepository, orderEventNotifier);
        }

        @Test
        void throwsNotFoundWhenTableIsInactive() {
            TableEntity inactiveTable = aTable().withId(tableId).withVenueId(venueId)
                    .withStatus(TableStatus.INACTIVE).build();
            when(tableRepository.findById(tableId)).thenReturn(Optional.of(inactiveTable));

            assertThatThrownBy(() -> createOrderUseCase.execute(
                    tableId, new CreateOrderRequest(List.of(new CreateOrderRequest.ItemDto(menuItemId, 1)))))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(orderRepository, menuItemRepository, orderEventNotifier);
        }

        @Test
        void throwsNotFoundWhenMenuItemIsMissing() {
            when(tableRepository.findById(tableId)).thenReturn(Optional.of(anActiveTable()));
            when(menuItemRepository.findAllById(List.of(menuItemId))).thenReturn(List.of());
            when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            assertThatThrownBy(() -> createOrderUseCase.execute(
                    tableId, new CreateOrderRequest(List.of(new CreateOrderRequest.ItemDto(menuItemId, 1)))))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(orderRepository).save(any(OrderEntity.class));
            verifyNoInteractions(orderItemRepository, orderEventNotifier);
        }

        @Test
        void throwsNotFoundWhenMenuItemIsDeleted() {
            MenuItemEntity deletedItem = aMenuItem().withId(menuItemId).withVenueId(venueId)
                    .withPrice(Money.of("5.00")).withStatus(MenuItemStatus.DELETED).build();
            when(tableRepository.findById(tableId)).thenReturn(Optional.of(anActiveTable()));
            when(menuItemRepository.findAllById(List.of(menuItemId))).thenReturn(List.of(deletedItem));
            when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            assertThatThrownBy(() -> createOrderUseCase.execute(
                    tableId, new CreateOrderRequest(List.of(new CreateOrderRequest.ItemDto(menuItemId, 1)))))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(orderRepository).save(any(OrderEntity.class));
            verifyNoInteractions(orderItemRepository, orderEventNotifier);
        }

        @Test
        void throwsNotFoundWhenMenuItemBelongsToDifferentVenue() {
            MenuItemEntity otherVenueItem = aMenuItem().withId(menuItemId).withVenueId(UUID.randomUUID())
                    .withPrice(Money.of("5.00")).build();
            when(tableRepository.findById(tableId)).thenReturn(Optional.of(anActiveTable()));
            when(menuItemRepository.findAllById(List.of(menuItemId))).thenReturn(List.of(otherVenueItem));
            when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            assertThatThrownBy(() -> createOrderUseCase.execute(
                    tableId, new CreateOrderRequest(List.of(new CreateOrderRequest.ItemDto(menuItemId, 1)))))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(orderRepository).save(any(OrderEntity.class));
            verifyNoInteractions(orderItemRepository, orderEventNotifier);
        }
    }

    @Nested
    class ListOrders {

        @Test
        void returnsOrdersWithGroupedItemsForActiveTable() {
            when(tableRepository.findById(tableId)).thenReturn(Optional.of(anActiveTable()));
            OrderEntity pendingOrder = anOrder().withId(orderId).withVenueId(venueId).withTableId(tableId)
                    .withStatus(OrderStatus.PENDING).build();
            OrderItemEntity lineItem = anOrderItem().withOrderId(orderId).withMenuItemId(menuItemId)
                    .withUnitPrice(Money.of("9.99")).build();
            when(orderRepository.findAllByTableIdOrderByCreatedAtDesc(tableId)).thenReturn(List.of(pendingOrder));
            when(orderItemRepository.findAllByOrderIdIn(List.of(orderId))).thenReturn(List.of(lineItem));

            List<OrderResponse> response = listOrdersUseCase.execute(tableId);

            assertThat(response).hasSize(1);
            assertThat(response.getFirst().id()).isEqualTo(orderId);
            assertThat(response.getFirst().items().getFirst().menuItemId()).isEqualTo(menuItemId);
        }

        @Test
        void returnsEmptyListWhenTableHasNoOrders() {
            when(tableRepository.findById(tableId)).thenReturn(Optional.of(anActiveTable()));
            when(orderRepository.findAllByTableIdOrderByCreatedAtDesc(tableId)).thenReturn(List.of());
            when(orderItemRepository.findAllByOrderIdIn(List.of())).thenReturn(List.of());

            List<OrderResponse> response = listOrdersUseCase.execute(tableId);

            assertThat(response).isEmpty();
        }

        @Test
        void throwsNotFoundWhenTableIsMissing() {
            when(tableRepository.findById(tableId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> listOrdersUseCase.execute(tableId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(orderRepository, orderItemRepository);
        }
    }

    private TableEntity anActiveTable() {
        return aTable().withId(tableId).withVenueId(venueId).build();
    }
}
