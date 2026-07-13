package com.milly.venue.infrastructure.adapter.outbound.persistence;

import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.MemberStatus;
import com.milly.venue.domain.valueobject.VenueRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VenueMembershipJpaRepository extends JpaRepository<VenueMembershipEntity, UUID> {

    Optional<VenueMembershipEntity> findByUserIdAndVenueId(UUID userId, UUID venueId);

    Optional<VenueMembershipEntity> findByIdAndVenueId(UUID id, UUID venueId);

    boolean existsByVenueIdAndUserIdAndRoleIn(UUID venueId, UUID userId, List<VenueRole> roles);

    List<VenueMembershipEntity> findAllByUserId(UUID userId);

    Page<VenueMembershipEntity> findAllByVenueIdOrderByCreatedAtAsc(UUID venueId, Pageable pageable);

    @Query("""
            SELECT m FROM VenueMembershipEntity m
            WHERE m.venueId = :venueId
              AND (:status IS NULL OR m.status = :status)
              AND (:role IS NULL OR m.role = :role)
            ORDER BY m.createdAt ASC
            """)
    Page<VenueMembershipEntity> findByVenueWithFilters(
            @Param("venueId") UUID venueId,
            @Param("status") MemberStatus status,
            @Param("role") VenueRole role,
            Pageable pageable);
}
