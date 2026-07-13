package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.AdminUserResponse;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.domain.valueobject.UserStatus;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserRoleJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserSpecifications;
import com.milly.common.application.dto.PageResponse;
import com.milly.common.application.dto.PaginationMeta;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListUsersUseCase {

    private final UserJpaRepository userRepository;
    private final UserRoleJpaRepository userRoleRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> execute(
            String cursor,
            int limit,
            UserStatus status,
            RoleName role,
            String email,
            String phoneNumber,
            String name,
            OffsetDateTime createdFrom,
            OffsetDateTime createdTo) {
        int safeLimit = Math.max(1, limit);
        int page = parseCursor(cursor);
        Pageable pageable = PageRequest.of(page, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<UserEntity> users = userRepository.findAll(
                UserSpecifications.withFilters(
                        status,
                        role,
                        blankToNull(email),
                        blankToNull(phoneNumber),
                        blankToNull(name),
                        createdFrom,
                        createdTo),
                pageable);

        List<UserEntity> content = users.getContent();
        Map<UUID, List<String>> rolesByUserId = loadRolesByUserId(
                content.stream().map(UserEntity::getId).toList());

        List<AdminUserResponse> response = content.stream()
                .map(user -> new AdminUserResponse(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getPhoneNumber(),
                        user.getStatus(),
                        user.getCreatedAt(),
                        rolesByUserId.getOrDefault(user.getId(), List.of())))
                .toList();

        return new PageResponse<>(response, new PaginationMeta(
                nextCursor(users),
                previousCursor(users),
                users.hasNext(),
                users.hasPrevious(),
                safeLimit));
    }

    private Map<UUID, List<String>> loadRolesByUserId(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, List<String>> rolesByUserId = new HashMap<>();
        for (Object[] row : userRoleRepository.findRoleNamesByUserIds(userIds)) {
            UUID userId = (UUID) row[0];
            RoleName roleName = (RoleName) row[1];
            rolesByUserId.computeIfAbsent(userId, ignored -> new ArrayList<>()).add(roleName.name());
        }
        return rolesByUserId;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private int parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }

        try {
            return Math.max(0, Integer.parseInt(cursor));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Cursor must be a non-negative integer page token.", ex);
        }
    }

    private String nextCursor(Page<UserEntity> users) {
        return users.hasNext() ? Integer.toString(users.getNumber() + 1) : null;
    }

    private String previousCursor(Page<UserEntity> users) {
        return users.hasPrevious() ? Integer.toString(users.getNumber() - 1) : null;
    }
}
