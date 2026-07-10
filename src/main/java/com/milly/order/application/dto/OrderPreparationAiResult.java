package com.milly.order.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderPreparationAiResult(
        int minutes,
        String value
) {
}