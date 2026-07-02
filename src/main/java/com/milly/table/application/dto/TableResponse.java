package com.milly.table.application.dto;

import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TableResponse(
        UUID id,
        UUID venueId,
        String label,
        TableStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static TableResponse of(TableEntity table) {
        return new TableResponse(
                table.getId(),
                table.getVenueId(),
                table.getLabel(),
                table.getStatus(),
                table.getCreatedAt(),
                table.getUpdatedAt());
    }
}
