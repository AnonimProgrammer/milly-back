package com.milly.venue.domain.entity;

import com.github.f4b6a3.ulid.UlidCreator;
import com.milly.venue.domain.valueobject.VenueStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "venues")
public class VenueEntity {

    @Id
    private UUID id = UlidCreator.getUlid().toUuid();

    @Column(nullable = false)
    private String name;

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VenueStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static VenueEntity create(String name, String location, VenueStatus status) {
        VenueEntity venue = new VenueEntity();
        venue.setName(name.trim());
        venue.setLocation(location.trim());
        venue.setStatus(status);
        return venue;
    }
}
