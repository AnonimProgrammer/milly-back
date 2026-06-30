package com.milly.auth.infrastructure.adapter.outbound.persistence;

import com.milly.auth.domain.entity.RoleEntity;
import com.milly.auth.domain.valueobject.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByName(RoleName name);
}
