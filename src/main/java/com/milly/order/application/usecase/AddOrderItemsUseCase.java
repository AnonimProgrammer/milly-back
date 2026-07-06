package com.milly.order.application.usecase;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddOrderItemsUseCase {

    private final TableJpaRepository tableRepository;
    private final MenuItemJpaRepository menuItemRepository;
    private final OrderJpaRepository orderRepository;
    private final OrderItemJpaRepository orderItemRepository;
    private final OrderEventNotifier orderEventNotifier;

    @Transactional
    public OrderResponse execute(UUID tableId, UUID orderId, AddOrderItemsRequest request) {
        TableEntity table = tableRepository.findById(tableId)
                .filter(t -> t.getStatus() == TableStatus.ACTIVE)
                .orElseThrow(ResourceNotFoundException::new);

        OrderEntity order = orderRepository.findByIdAndTableId(orderId, tableId)
                .orElseThrow(ResourceNotFoundException::new);

        if (order.getStatus() != OrderStatus.APPROVED) {
            throw new InvalidStateTransitionException();
        }

        List<UUID> menuItemIds = request.items().stream()
                .map(CreateOrderRequest.ItemDto::menuItemId)
                .distinct()
                .toList();
        Map<UUID, MenuItemEntity> menuItems = menuItemRepository.findAllById(menuItemIds).stream()
                .collect(Collectors.toMap(MenuItemEntity::getId, m -> m));

        List<OrderItemEntity> newItems = request.items().stream()
                .map(dto -> toOrderItem(order.getId(), dto, menuItems, table.getVenueId()))
                .toList();
        orderItemRepository.saveAll(newItems);

        orderEventNotifier.orderUpdated(order.getId(), order.getVenueId(), order.getTableId());

        return OrderResponse.of(order, orderItemRepository.findAllByOrderId(order.getId()));
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
            throw new ResourceNotFoundException();
        }

        return OrderItemEntity.create(
                orderId,
                menuItem.getId(),
                dto.quantity(),
                menuItem.getPrice());
    }
}
