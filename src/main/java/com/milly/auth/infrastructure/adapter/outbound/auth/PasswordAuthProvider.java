package com.milly.auth.infrastructure.adapter.outbound.auth;

import com.milly.auth.application.model.ExternalIdentity;
import com.milly.auth.domain.Credentials;
import com.milly.auth.domain.entity.UserAuthEntity;
import com.milly.auth.domain.valueobject.AuthProviderType;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserAuthJpaRepository;
import com.milly.common.exception.InvalidCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PasswordAuthProvider {

    private final UserAuthJpaRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordAuthProvider(UserAuthJpaRepository userAuthRepository, PasswordEncoder passwordEncoder) {
        this.userAuthRepository = userAuthRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ExternalIdentity authenticate(Map<String, Object> credentials) {
        String identifier = Credentials.requiredNormalized(credentials, "username", "email");
        String password = Credentials.requiredRaw(credentials, "password");

        UserAuthEntity authEntry = userAuthRepository
                .findByProviderTypeAndProviderUserId(AuthProviderType.PASSWORD, identifier)
                .or(() -> userAuthRepository.findByProviderTypeAndEmail(AuthProviderType.PASSWORD, identifier))
                .orElse(null);

        if (authEntry == null) {
            return new ExternalIdentity(AuthProviderType.PASSWORD, identifier, identifier);
        }

        if (authEntry.getPasswordHash() == null || !passwordEncoder.matches(password, authEntry.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password.");
        }

        String providerUserId = authEntry.getProviderUserId() == null ? identifier : authEntry.getProviderUserId();
        return new ExternalIdentity(AuthProviderType.PASSWORD, providerUserId, authEntry.getEmail());
    }
}
