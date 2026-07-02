package com.milly.table.application.dto;

import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;

import java.util.UUID;

public record PublicTableResponse(
        UUID id,
        UUID venueId,
        String label,
        TableStatus status
) {
    public static PublicTableResponse of(TableEntity table) {
        return new PublicTableResponse(
                table.getId(),
                table.getVenueId(),
                table.getLabel(),
                table.getStatus());
    }
}
