package com.milly.venue.infrastructure.adapter.outbound.persistence;

import com.milly.venue.domain.entity.VenueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VenueJpaRepository extends JpaRepository<VenueEntity, UUID> {
}
