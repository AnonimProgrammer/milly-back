package com.milly.venue.infrastructure.adapter.outbound.persistence;

import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VenueMembershipJpaRepository extends JpaRepository<VenueMembershipEntity, UUID> {

    Optional<VenueMembershipEntity> findByUserIdAndVenueId( UUID userId,UUID venueId  );

    boolean existsByVenueIdAndUserIdAndRoleIn(UUID venueId, UUID userId, List<VenueRole> roles);

    List<VenueMembershipEntity> findAllByUserId(UUID userId);

    Page<VenueMembershipEntity> findAllByVenueIdOrderByCreatedAtAsc(UUID venueId, Pageable pageable);
}
