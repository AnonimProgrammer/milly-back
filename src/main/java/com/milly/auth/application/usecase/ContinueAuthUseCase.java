package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.ContinueAuthRequest;
import com.milly.auth.application.dto.ContinueAuthResponse;
import com.milly.auth.domain.model.AuthUser;
import com.milly.auth.domain.model.ExternalIdentity;
import com.milly.auth.domain.model.IdentityResolution;
import com.milly.auth.domain.model.IssuedRefreshToken;
import com.milly.auth.application.port.outbound.AuthProvider;
import com.milly.auth.application.port.outbound.RefreshTokenStore;
import com.milly.auth.application.usecase.factory.AuthProviderFactory;
import com.milly.auth.domain.Credentials;
import com.milly.auth.infrastructure.adapter.outbound.security.JwtTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContinueAuthUseCase {

    private final AuthProviderFactory providerFactory;
    private final ResolveIdentityUseCase resolveIdentityUseCase;
    private final LoadAuthUserUseCase loadAuthUserUseCase;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenStore refreshTokenStore;

    public ContinueAuthUseCase(
            AuthProviderFactory providerFactory,
            ResolveIdentityUseCase resolveIdentityUseCase,
            LoadAuthUserUseCase loadAuthUserUseCase,
            JwtTokenService jwtTokenService,
            RefreshTokenStore refreshTokenStore) {
        this.providerFactory = providerFactory;
        this.resolveIdentityUseCase = resolveIdentityUseCase;
        this.loadAuthUserUseCase = loadAuthUserUseCase;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenStore = refreshTokenStore;
    }

    @Transactional
    public ContinueAuthResponse execute(ContinueAuthRequest request) {
        AuthProvider provider = providerFactory.get(request.provider());
        ExternalIdentity identity = provider.authenticate(request.credentials());

        String rawPassword = Credentials.optionalRaw(request.credentials(), "password");

        IdentityResolution resolution = resolveIdentityUseCase.execute(identity, request.profile(), rawPassword);

        AuthUser authUser = loadAuthUserUseCase.execute(resolution.user());

        String accessToken = jwtTokenService.issueAccessToken(authUser);
        IssuedRefreshToken refreshToken = jwtTokenService.issueRefreshToken(authUser);
        refreshTokenStore.register(refreshToken.jti(), authUser.id());

        return new ContinueAuthResponse(accessToken, refreshToken.token(), resolution.newUser());
    }
}
