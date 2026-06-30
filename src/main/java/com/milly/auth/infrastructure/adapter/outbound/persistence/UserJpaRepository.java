package com.milly.auth.infrastructure.adapter.outbound.persistence;

import com.milly.auth.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
}
