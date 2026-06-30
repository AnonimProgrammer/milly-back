package com.milly.auth.application.port.outbound;

import com.milly.auth.domain.model.ExternalIdentity;
import com.milly.auth.domain.valueobject.AuthProviderType;

import java.util.Map;

public interface AuthProvider {

    AuthProviderType getType();

    ExternalIdentity authenticate(Map<String, Object> credentials);
}
