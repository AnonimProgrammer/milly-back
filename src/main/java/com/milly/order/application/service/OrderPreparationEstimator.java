package com.milly.order.application.service;

import com.milly.common.exception.AiServiceUnavailableException;
import com.milly.common.exception.ResourceNotFoundException;
import com.milly.config.application.port.outbound.OrderPreparationAnalysisPort;
import com.milly.menu.domain.entity.MenuItemEntity;
import com.milly.menu.infrastructure.adapter.outbound.persistence.MenuItemJpaRepository;
import com.milly.order.application.dto.OrderPreparationAiResult;
import com.milly.order.application.dto.OrderPreparationAnalysisPayload;
import com.milly.order.domain.entity.OrderItemEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPreparationEstimator {

    private final MenuItemJpaRepository menuItemRepository;
    private final OrderPreparationAnalysisPort orderPreparationAnalysisPort;
    private final OrderPreparationAiMapper orderPreparationAiMapper;
    private final ObjectMapper objectMapper;

    public Optional<OrderPreparationAiResult> tryEstimate(
            UUID venueId,
            UUID orderId,
            List<OrderItemEntity> orderItems) {
        try {
            return Optional.of(estimate(venueId, orderId, orderItems));
        } catch (Exception exception) {
            log.warn("Failed to estimate preparation time for order {}: {}", orderId, exception.getMessage());
            return Optional.empty();
        }
    }

    public OrderPreparationAiResult estimate(UUID venueId, UUID orderId, List<OrderItemEntity> orderItems) {
        if (orderItems.isEmpty()) {
            throw new IllegalArgumentException("Order has no items to estimate preparation time for.");
        }

        Map<UUID, MenuItemEntity> menuItemsById = loadMenuItems(venueId, orderItems);
        String payload = buildPayload(orderId, orderItems, menuItemsById);

        var aiResponse = orderPreparationAnalysisPort.estimatePreparationTime(payload);
        var aiResult = orderPreparationAiMapper.toResult(aiResponse);

        if (aiResult.minutes() <= 0 || aiResult.value() == null || aiResult.value().isBlank()) {
            throw new IllegalArgumentException("AI returned an invalid preparation time estimate.");
        }

        return aiResult;
    }

    private Map<UUID, MenuItemEntity> loadMenuItems(UUID venueId, List<OrderItemEntity> orderItems) {
        List<UUID> menuItemIds = orderItems.stream()
                .map(OrderItemEntity::getMenuItemId)
                .distinct()
                .toList();

        Map<UUID, MenuItemEntity> menuItemsById = menuItemRepository.findAllByIdInAndVenueId(menuItemIds, venueId)
                .stream()
                .collect(Collectors.toMap(MenuItemEntity::getId, Function.identity()));

        if (menuItemsById.size() != menuItemIds.size()) {
            throw new ResourceNotFoundException();
        }

        return menuItemsById;
    }

    private String buildPayload(
            UUID orderId,
            List<OrderItemEntity> orderItems,
            Map<UUID, MenuItemEntity> menuItemsById) {
        try {
            List<OrderPreparationAnalysisPayload.Item> items = orderItems.stream()
                    .map(orderItem -> {
                        MenuItemEntity menuItem = menuItemsById.get(orderItem.getMenuItemId());
                        return new OrderPreparationAnalysisPayload.Item(
                                menuItem.getName(),
                                orderItem.getQuantity(),
                                menuItem.getApproximatePreparationMinutes());
                    })
                    .toList();

            int totalQuantity = items.stream()
                    .mapToInt(OrderPreparationAnalysisPayload.Item::quantity)
                    .sum();

            return objectMapper.writeValueAsString(new OrderPreparationAnalysisPayload(
                    orderId, items.size(), totalQuantity, items));
        } catch (Exception exception) {
            throw new AiServiceUnavailableException("Failed to prepare order data for AI analysis.", exception);
        }
    }
}