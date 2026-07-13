package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.AdminUserResponse;
import com.milly.auth.application.dto.UpdateAdminUserRequest;
import com.milly.auth.domain.entity.RoleEntity;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.entity.UserRoleEntity;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.domain.valueobject.UserStatus;
import com.milly.auth.infrastructure.adapter.outbound.persistence.RoleJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserRoleJpaRepository;
import com.milly.common.application.exception.AccessDeniedException;
import com.milly.common.application.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateAdminUserUseCase {

    private final UserJpaRepository userRepository;
    private final UserRoleJpaRepository userRoleRepository;
    private final RoleJpaRepository roleRepository;

    @Transactional
    public AdminUserResponse execute(UUID actorUserId, UUID targetUserId, UpdateAdminUserRequest request) {
        if (request.status() == null && request.roles() == null) {
            throw new IllegalArgumentException("At least one of status or roles must be provided.");
        }

        UserEntity target = userRepository.findById(targetUserId)
                .orElseThrow(ResourceNotFoundException::new);

        if (request.status() != null) {
            applyStatus(actorUserId, target, request.status());
        }

        List<RoleName> roles = request.roles() == null
                ? userRoleRepository.findRoleNamesByUserId(targetUserId)
                : replaceRoles(actorUserId, targetUserId, request.roles());

        UserEntity saved = userRepository.save(target);
        return new AdminUserResponse(
                saved.getId(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getEmail(),
                saved.getPhoneNumber(),
                saved.getStatus(),
                saved.getCreatedAt(),
                roles.stream().map(RoleName::name).toList());
    }

    private void applyStatus(UUID actorUserId, UserEntity target, UserStatus status) {
        if (actorUserId.equals(target.getId()) && status != UserStatus.ACTIVE) {
            throw new AccessDeniedException();
        }
        target.setStatus(status);
    }

    private List<RoleName> replaceRoles(UUID actorUserId, UUID targetUserId, List<RoleName> requestedRoles) {
        if (requestedRoles.isEmpty()) {
            throw new IllegalArgumentException("Roles must not be empty.");
        }

        Set<RoleName> nextRoles = new LinkedHashSet<>(requestedRoles);
        nextRoles.add(RoleName.USER);

        if (actorUserId.equals(targetUserId) && !nextRoles.contains(RoleName.ADMIN)) {
            throw new AccessDeniedException();
        }

        Set<RoleName> knownRoles = EnumSet.allOf(RoleName.class);
        for (RoleName role : nextRoles) {
            if (!knownRoles.contains(role)) {
                throw new IllegalArgumentException("Unknown role: " + role);
            }
        }

        userRoleRepository.deleteByUserId(targetUserId);
        for (RoleName roleName : nextRoles) {
            RoleEntity role = roleRepository.findByName(roleName)
                    .orElseThrow(ResourceNotFoundException::new);
            userRoleRepository.save(new UserRoleEntity(targetUserId, role.getId()));
        }

        return List.copyOf(nextRoles);
    }
}
