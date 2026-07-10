package com.milly.auth.infrastructure.adapter.outbound.venue;

import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.valueobject.UserStatus;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserJpaRepository;
import com.milly.venue.application.port.outbound.UserProfilePort;
import com.milly.venue.domain.valueobject.MemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserProfileAdapter implements UserProfilePort {

    private final UserJpaRepository userRepository;

    @Override
    public Map<UUID, UserProfileSummary> findByIds(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, this::toSummary));
    }

    private UserProfileSummary toSummary(UserEntity user) {
        return new UserProfileSummary(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                mapStatus(user.getStatus()));
    }

    private MemberStatus mapStatus(UserStatus status) {
        return switch (status) {
            case ACTIVE -> MemberStatus.ACTIVE;
            case INACTIVE, SUSPENDED -> MemberStatus.INACTIVE;
        };
    }
}
