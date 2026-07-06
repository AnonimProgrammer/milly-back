package com.milly.order.application.service;

import com.milly.config.websocket.StompTopics;
import com.milly.config.websocket.WsEventPublisher;
import com.milly.order.application.dto.OrderEvent;
import com.milly.order.application.dto.OrderEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderEventNotifier {

    private final WsEventPublisher wsEventPublisher;

    public void orderPlaced(UUID orderId, UUID venueId, UUID tableId) {
        publish(OrderEventType.ORDER_PLACED, orderId, venueId, tableId, StompTopics.venueStaffTopic(venueId));
    }

    public void orderApproved(UUID orderId, UUID venueId, UUID tableId) {
        publish(
                OrderEventType.ORDER_APPROVED,
                orderId,
                venueId,
                tableId,
                StompTopics.tableTopic(tableId),
                StompTopics.venueStaffTopic(venueId));
    }

    public void orderRejected(UUID orderId, UUID venueId, UUID tableId) {
        publish(
                OrderEventType.ORDER_REJECTED,
                orderId,
                venueId,
                tableId,
                StompTopics.tableTopic(tableId),
                StompTopics.venueStaffTopic(venueId));
    }

    public void orderClosed(UUID orderId, UUID venueId, UUID tableId) {
        publish(
                OrderEventType.ORDER_CLOSED,
                orderId,
                venueId,
                tableId,
                StompTopics.tableTopic(tableId),
                StompTopics.venueStaffTopic(venueId));
    }

    public void orderUpdated(UUID orderId, UUID venueId, UUID tableId) {
        publish(OrderEventType.ORDER_UPDATED, orderId, venueId, tableId, StompTopics.venueStaffTopic(venueId));
    }

    private void publish(OrderEventType type, UUID orderId, UUID venueId, UUID tableId, String... destinations) {
        OrderEvent event = new OrderEvent(type, orderId, venueId, tableId);
        for (String destination : destinations) {
            wsEventPublisher.publishAfterCommit(destination, event);
        }
    }
}
