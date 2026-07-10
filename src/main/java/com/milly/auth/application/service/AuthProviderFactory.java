package com.milly.auth.application.service;

import com.milly.auth.application.port.outbound.AuthProvider;
import com.milly.auth.domain.valueobject.AuthProviderType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AuthProviderFactory {

    private final Map<AuthProviderType, AuthProvider> providers;

    public AuthProviderFactory(List<AuthProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(AuthProvider::getType, Function.identity()));
    }

    public AuthProvider get(AuthProviderType type) {
        AuthProvider provider = providers.get(type);
        if (provider == null) {
            throw new UnsupportedOperationException("Unsupported auth provider: " + type);
        }
        return provider;
    }
}
