package com.milly.auth.domain.entity;

import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_auth")
public class UserAuthEntity {

    @Id
    private UUID id = UlidCreator.getUlid().toUuid();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "provider_user_id")
    private String providerUserId;

    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public static UserAuthEntity create(
            UUID userId,
            UUID providerId,
            String providerUserId,
            String email,
            String passwordHash) {
        UserAuthEntity userAuth = new UserAuthEntity();
        userAuth.setUserId(userId);
        userAuth.setProviderId(providerId);
        userAuth.setProviderUserId(providerUserId);
        userAuth.setEmail(email);
        userAuth.setPasswordHash(passwordHash);
        return userAuth;
    }
}
