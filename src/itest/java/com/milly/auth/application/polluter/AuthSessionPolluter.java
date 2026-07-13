package com.milly.auth.application.polluter;

import com.milly.auth.application.dto.ContinueAuthRequest;
import com.milly.auth.application.port.outbound.SessionTokenPort;
import com.milly.auth.application.usecase.CreateUserUseCase;
import com.milly.auth.application.usecase.LoadAuthUserUseCase;
import com.milly.auth.domain.entity.RoleEntity;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.entity.UserRoleEntity;
import com.milly.auth.domain.model.ExternalIdentity;
import com.milly.auth.domain.valueobject.AuthProviderType;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.infrastructure.adapter.outbound.persistence.RoleJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserRoleJpaRepository;
import com.milly.common.application.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthSessionPolluter {

    private static final String DEFAULT_PASSWORD = "password123";

    private final CreateUserUseCase createUserUseCase;
    private final LoadAuthUserUseCase loadAuthUserUseCase;
    private final SessionTokenPort sessionTokenPort;
    private final UserJpaRepository userRepository;
    private final RoleJpaRepository roleRepository;
    private final UserRoleJpaRepository userRoleRepository;

    public AuthSession registerPasswordUser() {
        return registerPasswordUser(uniqueEmail(), DEFAULT_PASSWORD);
    }

    public AuthSession registerPasswordUser(String email, String password) {
        UserEntity user = createUserUseCase.execute(
                new ExternalIdentity(AuthProviderType.PASSWORD, email, email),
                new ContinueAuthRequest.UserProfileDto("Test", "User", email),
                password);
        String accessToken = sessionTokenPort.issueAccessToken(loadAuthUserUseCase.execute(user));
        return new AuthSession(user.getId(), email, password, accessToken);
    }

    public AuthSession registerAdminUser() {
        return grantAdminRole(registerPasswordUser());
    }

    public AuthSession grantAdminRole(AuthSession session) {
        RoleEntity adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(ResourceNotFoundException::new);
        userRoleRepository.save(new UserRoleEntity(session.userId(), adminRole.getId()));

        UserEntity user = userRepository.findById(session.userId())
                .orElseThrow(ResourceNotFoundException::new);
        String accessToken = sessionTokenPort.issueAccessToken(loadAuthUserUseCase.execute(user));
        return new AuthSession(session.userId(), session.email(), session.password(), accessToken);
    }

    private static String uniqueEmail() {
        return "itest-" + UUID.randomUUID() + "@example.com";
    }
}
