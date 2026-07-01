package com.milly.venue.infrastructure.adapter.outbound.persistence;

import com.milly.venue.domain.entity.VenueMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VenueMembershipJpaRepository extends JpaRepository<VenueMembershipEntity, UUID> {

    Optional<VenueMembershipEntity> findByUserIdAndVenueId(UUID userId, UUID venueId);
}
