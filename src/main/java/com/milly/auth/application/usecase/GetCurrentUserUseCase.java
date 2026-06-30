package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.CurrentUserResponse;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserRoleJpaRepository;
import com.milly.common.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GetCurrentUserUseCase {

    private final UserJpaRepository userRepository;
    private final UserRoleJpaRepository userRoleRepository;

    public GetCurrentUserUseCase(
            UserJpaRepository userRepository,
            UserRoleJpaRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Transactional
    public CurrentUserResponse execute(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        List<RoleName> roles = userRoleRepository.findRoleNamesByUserId(userId);
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roles.stream()
                        .map(RoleName::name)
                        .toList()
        );
    }
}