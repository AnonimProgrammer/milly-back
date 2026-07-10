package com.milly.auth.application.usecase.builder;

import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.valueobject.UserStatus;

import java.util.UUID;

public final class UserTestBuilder {

    private UUID id = UUID.randomUUID();
    private String firstName = "Jane";
    private String lastName = "Doe";
    private String email = "jane.doe@example.com";

    private UserTestBuilder() {
    }

    public static UserTestBuilder aUser() {
        return new UserTestBuilder();
    }

    public UserTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public UserTestBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public UserTestBuilder withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public UserTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserEntity build() {
        UserEntity user = UserEntity.createActive(firstName, lastName, email);
        user.setId(id);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}