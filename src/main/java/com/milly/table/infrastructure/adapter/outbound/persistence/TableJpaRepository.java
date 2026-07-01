package com.milly.table.infrastructure.adapter.outbound.persistence;

import com.milly.table.domain.entity.TableEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TableJpaRepository extends JpaRepository<TableEntity, UUID> {

    List<TableEntity> findByVenueIdOrderByLabelAsc(UUID venueId);

    Optional<TableEntity> findByIdAndVenueId(UUID id, UUID venueId);
}
