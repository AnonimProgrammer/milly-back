package com.milly.table.application.usecase;

import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;

import java.util.UUID;

final class TableTestBuilder {

    private UUID id = UUID.randomUUID();
    private UUID venueId = UUID.randomUUID();
    private String label = "T1";
    private TableStatus status = TableStatus.ACTIVE;

    private TableTestBuilder() {
    }

    static TableTestBuilder aTable() {
        return new TableTestBuilder();
    }

    TableTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    TableTestBuilder withVenueId(UUID venueId) {
        this.venueId = venueId;
        return this;
    }

    TableTestBuilder withLabel(String label) {
        this.label = label;
        return this;
    }

    TableTestBuilder withStatus(TableStatus status) {
        this.status = status;
        return this;
    }

    TableEntity build() {
        TableEntity table = TableEntity.create(venueId, label, status);
        table.setId(id);
        return table;
    }
}
