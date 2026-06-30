package com.milly.auth.application.dto;

import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    List<String> roles
) {}