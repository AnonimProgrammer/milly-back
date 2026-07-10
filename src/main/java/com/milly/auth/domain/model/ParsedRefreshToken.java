package com.milly.auth.domain.model;

import java.util.UUID;

public record ParsedRefreshToken(UUID userId, String jti) {
}
