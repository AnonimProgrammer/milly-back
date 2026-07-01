package com.milly.table.domain.entity;

import com.github.f4b6a3.ulid.UlidCreator;
import com.milly.table.domain.valueobject.TableStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tables")
public class TableEntity {

    @Id
    private UUID id = UlidCreator.getUlid().toUuid();

    @Column(name = "venue_id", nullable = false)
    private UUID venueId;

    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TableStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static TableEntity createActive(UUID venueId, String label) {
        TableEntity table = new TableEntity();
        table.setVenueId(venueId);
        table.setLabel(label);
        table.setStatus(TableStatus.ACTIVE);
        return table;
    }

    public void deactivate() {
        this.status = TableStatus.INACTIVE;
    }
}
