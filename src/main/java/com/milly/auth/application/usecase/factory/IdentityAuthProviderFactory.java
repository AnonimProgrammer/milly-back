package com.milly.auth.application.usecase.factory;

import com.milly.auth.application.model.ExternalIdentity;
import com.milly.auth.domain.valueobject.AuthProviderType;
import com.milly.auth.infrastructure.adapter.outbound.auth.AppleAuthProvider;
import com.milly.auth.infrastructure.adapter.outbound.auth.GoogleAuthProvider;
import com.milly.auth.infrastructure.adapter.outbound.auth.PasswordAuthProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class IdentityAuthProviderFactory {

    private final PasswordAuthProvider passwordAuthProvider;
    private final GoogleAuthProvider googleAuthProvider;
    private final AppleAuthProvider appleAuthProvider;

    public IdentityAuthProviderFactory(
            PasswordAuthProvider passwordAuthProvider,
            GoogleAuthProvider googleAuthProvider,
            AppleAuthProvider appleAuthProvider) {
        this.passwordAuthProvider = passwordAuthProvider;
        this.googleAuthProvider = googleAuthProvider;
        this.appleAuthProvider = appleAuthProvider;
    }

    public ExternalIdentity authenticate(AuthProviderType type, Map<String, Object> credentials) {
        return switch (type) {
            case PASSWORD -> passwordAuthProvider.authenticate(credentials);
            case GOOGLE -> googleAuthProvider.authenticate(credentials);
            case APPLE -> appleAuthProvider.authenticate(credentials);
        };
    }
}
