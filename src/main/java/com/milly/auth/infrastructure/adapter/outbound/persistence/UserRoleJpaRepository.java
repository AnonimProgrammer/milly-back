package com.milly.auth.infrastructure.adapter.outbound.persistence;

import com.milly.auth.domain.entity.UserRoleEntity;
import com.milly.auth.domain.entity.UserRoleId;
import com.milly.auth.domain.valueobject.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRoleJpaRepository extends JpaRepository<UserRoleEntity, UserRoleId> {

    @Query("""
            SELECT r.name FROM UserRoleEntity ur
            JOIN RoleEntity r ON ur.roleId = r.id
            WHERE ur.userId = :userId
            """)
    List<RoleName> findRoleNamesByUserId(@Param("userId") UUID userId);
}
