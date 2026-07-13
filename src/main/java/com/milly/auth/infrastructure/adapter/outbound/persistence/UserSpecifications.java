package com.milly.auth.infrastructure.adapter.outbound.persistence;

import com.milly.auth.domain.entity.RoleEntity;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.entity.UserRoleEntity;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.domain.valueobject.UserStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<UserEntity> withFilters(
            UserStatus status,
            RoleName role,
            String email,
            String phoneNumber,
            String name,
            OffsetDateTime createdFrom,
            OffsetDateTime createdTo) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            if (status != null) {
                predicates = cb.and(predicates, cb.equal(root.get("status"), status));
            }

            if (email != null) {
                predicates = cb.and(
                        predicates,
                        cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
            }

            if (phoneNumber != null) {
                predicates = cb.and(
                        predicates,
                        cb.like(root.get("phoneNumber"), "%" + phoneNumber + "%"));
            }

            if (name != null) {
                var fullName = cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName")));
                predicates = cb.and(predicates, cb.like(fullName, "%" + name.toLowerCase() + "%"));
            }

            if (createdFrom != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }

            if (createdTo != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }

            if (role != null && query != null) {
                var roleExists = query.subquery(Integer.class);
                var userRole = roleExists.from(UserRoleEntity.class);
                var roleEntity = roleExists.from(RoleEntity.class);
                roleExists.select(cb.literal(1));
                roleExists.where(
                        cb.equal(userRole.get("userId"), root.get("id")),
                        cb.equal(userRole.get("roleId"), roleEntity.get("id")),
                        cb.equal(roleEntity.get("name"), role));
                predicates = cb.and(predicates, cb.exists(roleExists));
            }

            return predicates;
        };
    }
}
