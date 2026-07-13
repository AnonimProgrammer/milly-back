package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.CurrentUserResponse;
import com.milly.auth.application.dto.UpdateCurrentUserRequest;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.model.AuthUser;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserJpaRepository;
import com.milly.common.application.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateCurrentUserUseCase {

    private final UserJpaRepository userRepository;
    private final LoadAuthUserUseCase loadAuthUserUseCase;

    @Transactional
    public CurrentUserResponse execute(UUID userId, UpdateCurrentUserRequest request) {
        if (request.firstName() == null && request.lastName() == null && request.phoneNumber() == null) {
            throw new IllegalArgumentException("At least one of firstName, lastName, or phoneNumber must be provided.");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(ResourceNotFoundException::new);

        if (request.firstName() != null) {
            String firstName = request.firstName().trim();
            if (firstName.isEmpty()) {
                throw new IllegalArgumentException("First name must not be blank.");
            }
            user.setFirstName(firstName);
        }
        if (request.lastName() != null) {
            String lastName = request.lastName().trim();
            if (lastName.isEmpty()) {
                throw new IllegalArgumentException("Last name must not be blank.");
            }
            user.setLastName(lastName);
        }
        if (request.phoneNumber() != null) {
            String phone = request.phoneNumber().trim();
            user.setPhoneNumber(phone.isEmpty() ? null : phone);
        }

        UserEntity saved = userRepository.save(user);
        AuthUser authUser = loadAuthUserUseCase.execute(saved);
        return new CurrentUserResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getPhoneNumber(),
                authUser.roles().stream()
                        .map(RoleName::name)
                        .toList()
        );
    }
}
