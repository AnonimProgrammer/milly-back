package com.milly.order.application.service;

import com.milly.config.domain.constant.StompTopics;
import com.milly.config.application.port.outbound.WsEventPublisher;
import com.milly.order.application.dto.OrderEvent;
import com.milly.order.application.dto.OrderEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventNotifierTest {

    @Mock
    private WsEventPublisher wsEventPublisher;

    @InjectMocks
    private OrderEventNotifier orderEventNotifier;

    @Test
    void orderPlacedPublishesToVenueStaffTopicOnly() {
        UUID orderId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();

        orderEventNotifier.orderPlaced(orderId, venueId, tableId);

        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OrderEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(wsEventPublisher).publishAfterCommit(destinationCaptor.capture(), eventCaptor.capture());

        assertThat(destinationCaptor.getAllValues()).containsExactly(StompTopics.venueStaffTopic(venueId));
        assertThat(eventCaptor.getValue()).isEqualTo(new OrderEvent(OrderEventType.ORDER_PLACED, orderId, venueId, tableId));
    }

    @Test
    void orderApprovedPublishesToTableAndVenueStaffTopics() {
        UUID orderId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();

        orderEventNotifier.orderApproved(orderId, venueId, tableId);

        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        verify(wsEventPublisher, times(2)).publishAfterCommit(destinationCaptor.capture(), org.mockito.ArgumentMatchers.any());
        assertThat(destinationCaptor.getAllValues()).containsExactly(
                StompTopics.tableTopic(tableId),
                StompTopics.venueStaffTopic(venueId));
    }

    @Test
    void paymentReceivedPublishesToTableAndVenueStaffTopics() {
        UUID orderId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();

        orderEventNotifier.paymentReceived(orderId, venueId, tableId);

        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        verify(wsEventPublisher, times(2)).publishAfterCommit(destinationCaptor.capture(), org.mockito.ArgumentMatchers.any());
        assertThat(destinationCaptor.getAllValues()).containsExactly(
                StompTopics.tableTopic(tableId),
                StompTopics.venueStaffTopic(venueId));
    }
}
