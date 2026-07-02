package com.milly.table.application.mapper;

import com.milly.table.application.dto.PublicTableResponse;
import com.milly.table.domain.entity.TableEntity;

public final class PublicTableResponseMapper {

    private PublicTableResponseMapper() {
    }

    public static PublicTableResponse toResponse(TableEntity table) {
        return new PublicTableResponse(
                table.getId(),
                table.getVenueId(),
                table.getLabel(),
                table.getStatus());
    }
}
