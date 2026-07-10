package com.milly.auth.application.service;

import com.milly.auth.application.port.outbound.AuthProvider;
import com.milly.auth.domain.valueobject.AuthProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthProviderFactoryTest {

    @Mock
    private AuthProvider passwordProvider;

    @Mock
    private AuthProvider googleProvider;

    private AuthProviderFactory authProviderFactory;

    @BeforeEach
    void setUp() {
        when(passwordProvider.getType()).thenReturn(AuthProviderType.PASSWORD);
        when(googleProvider.getType()).thenReturn(AuthProviderType.GOOGLE);
        authProviderFactory = new AuthProviderFactory(List.of(passwordProvider, googleProvider));
    }

    @Test
    void returnsRegisteredProviderForKnownType() {
        // Act & Assert
        assertThat(authProviderFactory.get(AuthProviderType.PASSWORD)).isSameAs(passwordProvider);
        assertThat(authProviderFactory.get(AuthProviderType.GOOGLE)).isSameAs(googleProvider);
    }

    @Test
    void throwsWhenProviderTypeIsNotRegistered() {
        // Act & Assert
        assertThatThrownBy(() -> authProviderFactory.get(AuthProviderType.APPLE))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Unsupported auth provider: APPLE");
    }
}