package com.milly.auth.application.usecase.builder;

import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.valueobject.UserStatus;

import java.util.UUID;

public final class UserTestBuilder {

    private UUID id = UUID.randomUUID();
    private String firstName = "Jane";
    private String lastName = "Doe";
    private String email = "jane.doe@example.com";
    private String phoneNumber;
    private UserStatus status = UserStatus.ACTIVE;

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

    public UserTestBuilder withPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public UserTestBuilder withStatus(UserStatus status) {
        this.status = status;
        return this;
    }

    public UserEntity build() {
        UserEntity user = UserEntity.createActive(firstName, lastName, email);
        user.setId(id);
        user.setPhoneNumber(phoneNumber);
        user.setStatus(status);
        return user;
    }
}