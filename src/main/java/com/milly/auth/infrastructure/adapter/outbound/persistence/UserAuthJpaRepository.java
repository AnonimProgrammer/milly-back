package com.milly.auth.infrastructure.adapter.outbound.persistence;

import com.milly.auth.domain.entity.UserAuthEntity;
import com.milly.auth.domain.valueobject.AuthProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserAuthJpaRepository extends JpaRepository<UserAuthEntity, UUID> {

    @Query("""
            SELECT ua FROM UserAuthEntity ua
            JOIN AuthProviderEntity ap ON ua.providerId = ap.id
            WHERE ap.type = :type AND ua.providerUserId = :providerUserId
            """)
    Optional<UserAuthEntity> findByProviderTypeAndProviderUserId(
            @Param("type") AuthProviderType type,
            @Param("providerUserId") String providerUserId);

    @Query("""
            SELECT ua FROM UserAuthEntity ua
            JOIN AuthProviderEntity ap ON ua.providerId = ap.id
            WHERE ap.type = :type AND LOWER(ua.email) = LOWER(:email)
            """)
    Optional<UserAuthEntity> findByProviderTypeAndEmail(
            @Param("type") AuthProviderType type,
            @Param("email") String email);
}
