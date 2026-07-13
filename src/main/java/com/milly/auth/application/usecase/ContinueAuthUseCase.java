package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.ContinueAuthRequest;
import com.milly.auth.application.dto.ContinueAuthResponse;
import com.milly.auth.application.port.outbound.AuthProvider;
import com.milly.auth.application.port.outbound.RefreshTokenStore;
import com.milly.auth.application.port.outbound.SessionTokenPort;
import com.milly.auth.application.service.AuthProviderFactory;
import com.milly.auth.domain.Credentials;
import com.milly.auth.domain.model.AuthUser;
import com.milly.auth.domain.model.ExternalIdentity;
import com.milly.auth.domain.model.IdentityResolution;
import com.milly.auth.domain.model.IssuedRefreshToken;
import com.milly.auth.domain.valueobject.AuthProviderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContinueAuthUseCase {

    private final AuthProviderFactory providerFactory;
    private final ResolveIdentityUseCase resolveIdentityUseCase;
    private final LoadAuthUserUseCase loadAuthUserUseCase;
    private final EnsureActiveUserUseCase ensureActiveUserUseCase;
    private final SessionTokenPort sessionTokenPort;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional
    public ContinueAuthResponse execute(ContinueAuthRequest request) {
        AuthProviderType providerType = request.provider();
        log.info("Authentication attempt. provider={}", providerType);

        AuthProvider provider = providerFactory.get(providerType);
        ExternalIdentity identity = provider.authenticate(request.credentials());

        String rawPassword = Credentials.optionalRaw(request.credentials(), "password");

        IdentityResolution resolution =
                resolveIdentityUseCase.execute(identity, request.profile(), rawPassword);

        ensureActiveUserUseCase.execute(resolution.user());

        AuthUser authUser = loadAuthUserUseCase.execute(resolution.user());

        String accessToken = sessionTokenPort.issueAccessToken(authUser);
        IssuedRefreshToken refreshToken = sessionTokenPort.issueRefreshToken(authUser);
        refreshTokenStore.register(refreshToken.jti(), authUser.id());

        log.info("Authentication succeeded. provider={} userId={}", providerType, authUser.id());

        return new ContinueAuthResponse(accessToken, refreshToken.token(), resolution.newUser());
    }
}