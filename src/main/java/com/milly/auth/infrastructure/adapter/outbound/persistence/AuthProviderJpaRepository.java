package com.milly.auth.infrastructure.adapter.outbound.persistence;

import com.milly.auth.domain.entity.AuthProviderEntity;
import com.milly.auth.domain.valueobject.AuthProviderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthProviderJpaRepository extends JpaRepository<AuthProviderEntity, UUID> {

    Optional<AuthProviderEntity> findByType(AuthProviderType type);
}
