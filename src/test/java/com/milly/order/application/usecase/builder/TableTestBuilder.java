package com.milly.order.application.usecase.builder;

import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;

import java.util.UUID;

public final class TableTestBuilder {

    private UUID id = UUID.randomUUID();
    private UUID venueId = UUID.randomUUID();
    private String label = "T1";
    private TableStatus status = TableStatus.ACTIVE;

    private TableTestBuilder() {
    }

    public static TableTestBuilder aTable() {
        return new TableTestBuilder();
    }

    public TableTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public TableTestBuilder withVenueId(UUID venueId) {
        this.venueId = venueId;
        return this;
    }

    public TableTestBuilder withStatus(TableStatus status) {
        this.status = status;
        return this;
    }

    public TableEntity build() {
        TableEntity table = TableEntity.create(venueId, label, status);
        table.setId(id);
        return table;
    }
}