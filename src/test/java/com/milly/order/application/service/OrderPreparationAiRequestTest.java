package com.milly.order.application.service;

import com.milly.config.application.dto.AiResponse;
import com.milly.order.application.dto.OrderPreparationAnalysisPayload;
import com.milly.order.application.dto.OrderPreparationAiResult;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPreparationAiRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OrderPreparationAiMapper mapper = new OrderPreparationAiMapper(objectMapper);

    @Test
    void payloadMatchesPromptSchema() throws Exception {
        UUID orderId = UUID.fromString("3f2a9c1e-8b4d-4f1a-9c2e-1a2b3c4d5e6f");
        var payload = new OrderPreparationAnalysisPayload(
                orderId,
                4,
                6,
                List.of(
                        new OrderPreparationAnalysisPayload.Item("Burger", 1, 15),
                        new OrderPreparationAnalysisPayload.Item("Fries", 1, 15),
                        new OrderPreparationAnalysisPayload.Item("Salad", 1, 15),
                        new OrderPreparationAnalysisPayload.Item("Soup", 3, 15)));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(payload));

        assertThat(json.get("orderId").asText()).isEqualTo(orderId.toString());
        assertThat(json.get("lineItemCount").asInt()).isEqualTo(4);
        assertThat(json.get("totalQuantity").asInt()).isEqualTo(6);
        assertThat(json.get("items")).hasSize(4);
        assertThat(json.get("items").get(3).get("quantity").asInt()).isEqualTo(3);
        assertThat(json.get("items").get(3).get("approximatePreparationMinutes").asInt()).isEqualTo(15);
    }

    @Test
    void mapperParsesPlainJsonResponse() {
        OrderPreparationAiResult result = mapper.toResult(
                new AiResponse("{\"minutes\":90,\"value\":\"1 hour 30 minutes\"}"));

        assertThat(result.minutes()).isEqualTo(90);
        assertThat(result.value()).isEqualTo("1 hour 30 minutes");
    }

    @Test
    void mapperParsesMarkdownWrappedJsonResponse() {
        OrderPreparationAiResult result = mapper.toResult(
                new AiResponse("```json\n{\"minutes\":15,\"value\":\"15 minutes\"}\n```"));

        assertThat(result.minutes()).isEqualTo(15);
        assertThat(result.value()).isEqualTo("15 minutes");
    }
}