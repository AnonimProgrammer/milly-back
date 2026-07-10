package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.ContinueAuthRequest;
import com.milly.auth.domain.entity.AuthProviderEntity;
import com.milly.auth.domain.entity.RoleEntity;
import com.milly.auth.domain.entity.UserAuthEntity;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.entity.UserRoleEntity;
import com.milly.auth.domain.model.ExternalIdentity;
import com.milly.auth.domain.valueobject.AuthProviderType;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.infrastructure.adapter.outbound.persistence.AuthProviderJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.persistence.RoleJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserAuthJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserRoleJpaRepository;
import com.milly.common.domain.Preconditions;
import com.milly.common.application.exception.InvalidCredentialsException;
import com.milly.common.application.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateUserUseCase {

    private final UserJpaRepository userRepository;
    private final UserAuthJpaRepository userAuthRepository;
    private final AuthProviderJpaRepository authProviderRepository;
    private final RoleJpaRepository roleRepository;
    private final UserRoleJpaRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserEntity execute(
            ExternalIdentity identity,
            ContinueAuthRequest.UserProfileDto profile,
            String rawPassword) {
        if (profile == null) {
            throw new InvalidCredentialsException("Profile data is required for first-time sign-in.");
        }

        AuthProviderEntity provider = authProviderRepository.findByType(identity.provider())
                .orElseThrow(ResourceNotFoundException::new);
        RoleEntity defaultRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(ResourceNotFoundException::new);

        UserEntity user = UserEntity.createActive(
                Preconditions.required(profile.firstName(), "Missing required field: firstName"),
                Preconditions.required(profile.lastName(), "Missing required field: lastName"),
                Preconditions.required(
                        Preconditions.firstNonBlank(profile.email(), identity.email()),
                        "Missing required field: email"));
        UserEntity savedUser = userRepository.save(user);

        userRoleRepository.save(new UserRoleEntity(savedUser.getId(), defaultRole.getId()));

        String passwordHash = identity.provider() == AuthProviderType.PASSWORD ? encodePassword(rawPassword) : null;
        UserAuthEntity userAuth = UserAuthEntity.create(
                savedUser.getId(),
                provider.getId(),
                identity.providerUserId(),
                Preconditions.firstNonBlank(identity.email(), profile.email()),
                passwordHash);
        userAuthRepository.save(userAuth);

        return savedUser;
    }

    private String encodePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new InvalidCredentialsException("Password is required for password registration.");
        }
        return passwordEncoder.encode(rawPassword);
    }
}