package com.milly.order.application.service;

import com.milly.common.application.exception.ResourceNotFoundException;
import com.milly.config.application.dto.AiResponse;
import com.milly.config.application.port.outbound.OrderPreparationAnalysisPort;
import com.milly.menu.domain.entity.MenuItemEntity;
import com.milly.menu.infrastructure.adapter.outbound.persistence.MenuItemJpaRepository;
import com.milly.order.application.dto.OrderPreparationAiResult;
import com.milly.order.domain.entity.OrderItemEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.milly.order.application.usecase.builder.MenuItemTestBuilder.aMenuItem;
import static com.milly.order.application.usecase.builder.OrderItemTestBuilder.anOrderItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPreparationEstimatorTest {

    @Mock
    private MenuItemJpaRepository menuItemRepository;
    @Mock
    private OrderPreparationAnalysisPort analysisPort;
    @Mock
    private OrderPreparationAiMapper aiMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OrderPreparationEstimator estimator;

    private final UUID venueId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID menuItemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        estimator = new OrderPreparationEstimator(menuItemRepository, analysisPort, aiMapper, objectMapper);
    }

    @Test
    void buildsPayloadAndReturnsValidAiEstimate() throws Exception {
        // Arrange
        OrderItemEntity orderItem = anOrderItem()
                .withOrderId(orderId)
                .withMenuItemId(menuItemId)
                .withQuantity(2)
                .build();
        MenuItemEntity menuItem = aMenuItem()
                .withId(menuItemId)
                .withVenueId(venueId)
                .build();
        AiResponse aiResponse = new AiResponse("{\"minutes\":30,\"value\":\"30 minutes\"}");
        OrderPreparationAiResult aiResult = new OrderPreparationAiResult(30, "30 minutes");
        when(menuItemRepository.findAllByIdInAndVenueId(List.of(menuItemId), venueId))
                .thenReturn(List.of(menuItem));
        when(analysisPort.estimatePreparationTime(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(aiResponse);
        when(aiMapper.toResult(aiResponse)).thenReturn(aiResult);

        // Act
        OrderPreparationAiResult result = estimator.estimate(venueId, orderId, List.of(orderItem));

        // Assert
        assertThat(result).isEqualTo(aiResult);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(analysisPort).estimatePreparationTime(payloadCaptor.capture());
        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
        assertThat(payload.get("orderId").asText()).isEqualTo(orderId.toString());
        assertThat(payload.get("lineItemCount").asInt()).isEqualTo(1);
        assertThat(payload.get("totalQuantity").asInt()).isEqualTo(2);
        assertThat(payload.get("items").get(0).get("name").asText()).isEqualTo("Burger");
        assertThat(payload.get("items").get(0).get("approximatePreparationMinutes").asInt()).isEqualTo(15);
    }

    @Test
    void rejectsOrderWithoutItems() {
        // Act & Assert
        assertThatThrownBy(() -> estimator.estimate(venueId, orderId, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order has no items to estimate preparation time for.");

        verifyNoInteractions(menuItemRepository, analysisPort, aiMapper);
    }

    @Test
    void throwsNotFoundWhenReferencedMenuItemIsMissing() {
        // Arrange
        OrderItemEntity orderItem = anOrderItem().withMenuItemId(menuItemId).build();
        when(menuItemRepository.findAllByIdInAndVenueId(List.of(menuItemId), venueId))
                .thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> estimator.estimate(venueId, orderId, List.of(orderItem)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(analysisPort, aiMapper);
    }

    @Test
    void rejectsInvalidAiEstimate() {
        // Arrange
        OrderItemEntity orderItem = anOrderItem().withMenuItemId(menuItemId).build();
        MenuItemEntity menuItem = aMenuItem().withId(menuItemId).withVenueId(venueId).build();
        AiResponse aiResponse = new AiResponse("{}");
        when(menuItemRepository.findAllByIdInAndVenueId(List.of(menuItemId), venueId))
                .thenReturn(List.of(menuItem));
        when(analysisPort.estimatePreparationTime(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(aiResponse);
        when(aiMapper.toResult(aiResponse)).thenReturn(new OrderPreparationAiResult(0, ""));

        // Act & Assert
        assertThatThrownBy(() -> estimator.estimate(venueId, orderId, List.of(orderItem)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("AI returned an invalid preparation time estimate.");
    }

    @Test
    void tryEstimateReturnsEmptyWhenEstimationFails() {
        // Act
        Optional<OrderPreparationAiResult> result = estimator.tryEstimate(venueId, orderId, List.of());

        // Assert
        assertThat(result).isEmpty();
    }
}
