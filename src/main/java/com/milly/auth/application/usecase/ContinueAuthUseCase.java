package com.milly.auth.application.usecase;

import com.milly.auth.application.dto.ContinueAuthRequest;
import com.milly.auth.application.dto.ContinueAuthResponse;
import com.milly.auth.application.model.AuthUser;
import com.milly.auth.application.model.ExternalIdentity;
import com.milly.auth.application.model.IdentityResolution;
import com.milly.auth.application.usecase.factory.IdentityAuthProviderFactory;
import com.milly.auth.domain.Credentials;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserRoleJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.security.JwtTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContinueAuthUseCase {

    private final IdentityAuthProviderFactory providerFactory;
    private final ResolveIdentityUseCase resolveIdentityUseCase;
    private final UserRoleJpaRepository userRoleRepository;
    private final JwtTokenService jwtTokenService;

    public ContinueAuthUseCase(
            IdentityAuthProviderFactory providerFactory,
            ResolveIdentityUseCase resolveIdentityUseCase,
            UserRoleJpaRepository userRoleRepository,
            JwtTokenService jwtTokenService) {
        this.providerFactory = providerFactory;
        this.resolveIdentityUseCase = resolveIdentityUseCase;
        this.userRoleRepository = userRoleRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public ContinueAuthResponse execute(ContinueAuthRequest request) {
        ExternalIdentity identity = providerFactory.authenticate(request.provider(), request.credentials());
        String rawPassword = Credentials.optionalRaw(request.credentials(), "password");

        IdentityResolution resolution = resolveIdentityUseCase.execute(identity, request.profile(), rawPassword);

        AuthUser authUser = toAuthUser(resolution.user());
        String accessToken = jwtTokenService.issueAccessToken(authUser);
        String refreshToken = jwtTokenService.issueRefreshToken(authUser);

        return new ContinueAuthResponse(accessToken, refreshToken, resolution.newUser());
    }

    private AuthUser toAuthUser(UserEntity user) {
        List<RoleName> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
        return new AuthUser(user.getId(), roles);
    }
}
