package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.CurrentUserResponse;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.model.AuthUser;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserJpaRepository;
import com.milly.common.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetCurrentUserUseCase {

    private final UserJpaRepository userRepository;
    private final LoadAuthUserUseCase loadAuthUserUseCase;

    @Transactional
    public CurrentUserResponse execute(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        AuthUser authUser = loadAuthUserUseCase.execute(user);
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                authUser.roles().stream()
                        .map(RoleName::name)
                        .toList()
        );
    }
}