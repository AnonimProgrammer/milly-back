package com.milly.table.application.dto;

import com.milly.table.domain.valueobject.TableStatus;

import java.util.UUID;

public record PublicTableResponse(
        UUID id,
        UUID venueId,
        String label,
        TableStatus status
) {
}
