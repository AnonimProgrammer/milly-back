package com.milly.table.infrastructure.adapter.outbound.persistence;

import com.milly.table.domain.entity.TableEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TableJpaRepository extends JpaRepository<TableEntity, UUID> {
}
