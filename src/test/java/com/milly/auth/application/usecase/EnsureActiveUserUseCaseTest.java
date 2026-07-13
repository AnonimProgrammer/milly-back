package com.milly.auth.application.usecase;

import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.valueobject.UserStatus;
import com.milly.common.application.exception.UserAccountInactiveException;
import org.junit.jupiter.api.Test;

import static com.milly.auth.application.usecase.builder.UserTestBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnsureActiveUserUseCaseTest {

    private final EnsureActiveUserUseCase ensureActiveUserUseCase = new EnsureActiveUserUseCase();

    @Test
    void passesWhenUserIsActive() {
        UserEntity user = aUser().withStatus(UserStatus.ACTIVE).build();

        assertThatCode(() -> ensureActiveUserUseCase.execute(user))
                .doesNotThrowAnyException();
    }

    @Test
    void throwsWhenUserIsInactive() {
        UserEntity user = aUser().withStatus(UserStatus.INACTIVE).build();

        assertThatThrownBy(() -> ensureActiveUserUseCase.execute(user))
                .isInstanceOf(UserAccountInactiveException.class);
    }

    @Test
    void throwsWhenUserIsSuspended() {
        UserEntity user = aUser().withStatus(UserStatus.SUSPENDED).build();

        assertThatThrownBy(() -> ensureActiveUserUseCase.execute(user))
                .isInstanceOf(UserAccountInactiveException.class);
    }
}
