package com.milly.order.application.usecase;

import com.milly.common.exception.ResourceNotFoundException;
import com.milly.menu.domain.entity.MenuItemEntity;
import com.milly.menu.domain.valueobject.MenuItemStatus;
import com.milly.menu.infrastructure.adapter.outbound.persistence.MenuItemJpaRepository;
import com.milly.order.application.dto.CreateOrderRequest;
import com.milly.order.application.dto.OrderResponse;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.entity.OrderItemEntity;
import com.milly.order.domain.valueobject.OrderStatus;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CreateOrderUseCase {

    private final TableJpaRepository tableRepository;
    private final MenuItemJpaRepository menuItemRepository;
    private final OrderJpaRepository orderRepository;
    private final OrderItemJpaRepository orderItemRepository;

    public CreateOrderUseCase(
            TableJpaRepository tableRepository,
            MenuItemJpaRepository menuItemRepository,
            OrderJpaRepository orderRepository,
            OrderItemJpaRepository orderItemRepository) {
        this.tableRepository = tableRepository;
        this.menuItemRepository = menuItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public OrderResponse execute(UUID tableId, CreateOrderRequest request) {
        TableEntity table = tableRepository.findById(tableId)
                .filter(t -> t.getStatus() == TableStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found."));

        List<UUID> menuItemIds = request.items().stream()
                .map(CreateOrderRequest.ItemDto::menuItemId)
                .distinct()
                .toList();
        Map<UUID, MenuItemEntity> menuItems = menuItemRepository.findAllById(menuItemIds).stream()
                .collect(Collectors.toMap(MenuItemEntity::getId, m -> m));

        OrderEntity order = OrderEntity.create(table.getVenueId(), table.getId(), OrderStatus.PENDING);
        OrderEntity savedOrder = orderRepository.save(order);

        List<OrderItemEntity> items = request.items().stream()
                .map(dto -> toOrderItem(savedOrder.getId(), dto, menuItems, table.getVenueId()))
                .toList();
        List<OrderItemEntity> savedItems = orderItemRepository.saveAll(items);

        return OrderResponse.of(savedOrder, savedItems);
    }

    private OrderItemEntity toOrderItem(
            UUID orderId,
            CreateOrderRequest.ItemDto dto,
            Map<UUID, MenuItemEntity> menuItems,
            UUID venueId) {
        MenuItemEntity menuItem = menuItems.get(dto.menuItemId());
        if (menuItem == null
                || menuItem.getStatus() != MenuItemStatus.ACTIVE
                || !menuItem.getVenueId().equals(venueId)) {
            throw new ResourceNotFoundException("Menu item not found: " + dto.menuItemId());
        }

        return OrderItemEntity.create(orderId, menuItem.getId(), dto.quantity(), menuItem.getPrice());
    }
}
