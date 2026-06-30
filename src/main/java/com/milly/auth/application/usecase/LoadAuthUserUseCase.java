package com.milly.auth.application.usecase;

import com.milly.auth.domain.model.AuthUser;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserRoleJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LoadAuthUserUseCase {

    private final UserRoleJpaRepository userRoleRepository;

    public LoadAuthUserUseCase(UserRoleJpaRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }

    @Transactional(readOnly = true)
    public AuthUser execute(UserEntity user) {
        List<RoleName> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
        return new AuthUser(user.getId(), roles);
    }
}
