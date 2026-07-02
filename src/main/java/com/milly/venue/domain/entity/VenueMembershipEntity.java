package com.milly.venue.domain.entity;

import com.github.f4b6a3.ulid.UlidCreator;
import com.milly.venue.domain.valueobject.VenueRole;
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

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "venue_memberships")
public class VenueMembershipEntity {

    @Id
    private UUID id = UlidCreator.getUlid().toUuid();

    @Column(name = "venue_id", nullable = false)
    private UUID venueId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VenueRole role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public static VenueMembershipEntity create(UUID venueId, UUID userId, VenueRole role) {
        VenueMembershipEntity membership = new VenueMembershipEntity();
        membership.setVenueId(venueId);
        membership.setUserId(userId);
        membership.setRole(role);
        return membership;
    }
}
