package com.milly.auth.application.polluter;

import com.milly.auth.application.dto.ContinueAuthRequest;
import com.milly.auth.application.usecase.CreateUserUseCase;
import com.milly.auth.application.usecase.LoadAuthUserUseCase;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.model.ExternalIdentity;
import com.milly.auth.domain.valueobject.AuthProviderType;
import com.milly.auth.application.port.outbound.SessionTokenPort;
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

    private static String uniqueEmail() {
        return "itest-" + UUID.randomUUID() + "@example.com";
    }
}