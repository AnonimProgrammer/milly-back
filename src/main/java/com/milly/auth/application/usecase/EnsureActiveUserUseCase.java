package com.milly.auth.application.usecase;

import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.valueobject.UserStatus;
import com.milly.common.application.exception.UserAccountInactiveException;
import org.springframework.stereotype.Service;

@Service
public class EnsureActiveUserUseCase {

    public void execute(UserEntity user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserAccountInactiveException();
        }
    }
}
