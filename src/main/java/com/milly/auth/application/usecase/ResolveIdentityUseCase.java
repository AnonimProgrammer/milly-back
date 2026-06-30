package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.ContinueAuthRequest;
import com.milly.auth.application.model.ExternalIdentity;
import com.milly.auth.application.model.IdentityResolution;
import com.milly.auth.domain.entity.UserAuthEntity;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.valueobject.AuthProviderType;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserAuthJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserJpaRepository;
import com.milly.common.exception.InvalidCredentialsException;
import com.milly.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResolveIdentityUseCase {

    private final UserAuthJpaRepository userAuthRepository;
    private final UserJpaRepository userRepository;
    private final CreateUserUseCase createUserUseCase;

    public ResolveIdentityUseCase(
            UserAuthJpaRepository userAuthRepository,
            UserJpaRepository userRepository,
            CreateUserUseCase createUserUseCase) {
        this.userAuthRepository = userAuthRepository;
        this.userRepository = userRepository;
        this.createUserUseCase = createUserUseCase;
    }

    @Transactional
    public IdentityResolution execute(
            ExternalIdentity identity,
            ContinueAuthRequest.UserProfileDto profile,
            String rawPassword) {
        return userAuthRepository
                .findByProviderTypeAndProviderUserId(identity.provider(), identity.providerUserId())
                .map(userAuth -> loadExistingUser(userAuth, identity))
                .orElseGet(() -> new IdentityResolution(
                        createUserUseCase.execute(identity, profile, rawPassword), true));
    }

    private IdentityResolution loadExistingUser(UserAuthEntity userAuth, ExternalIdentity identity) {
        UserEntity user = userRepository.findById(userAuth.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User account not found."));
        if (identity.provider() == AuthProviderType.PASSWORD && userAuth.getPasswordHash() == null) {
            throw new InvalidCredentialsException("Invalid username or password.");
        }
        return new IdentityResolution(user, false);
    }
}
