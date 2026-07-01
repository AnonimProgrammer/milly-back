package com.milly.table.application.mapper;

import com.milly.table.application.dto.TableResponse;
import com.milly.table.domain.entity.TableEntity;

public final class TableResponseMapper {

    private TableResponseMapper() {
    }

    public static TableResponse toResponse(TableEntity table) {
        return new TableResponse(
                table.getId(),
                table.getVenueId(),
                table.getLabel(),
                table.getStatus(),
                table.getCreatedAt(),
                table.getUpdatedAt());
    }
}
