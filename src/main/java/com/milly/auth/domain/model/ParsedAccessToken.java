package com.milly.auth.domain.model;

import java.util.List;
import java.util.UUID;

public record ParsedAccessToken(UUID userId, List<String> roles) {
}
