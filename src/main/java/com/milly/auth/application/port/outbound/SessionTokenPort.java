package com.milly.auth.application.port.outbound;

import com.milly.auth.domain.model.AuthUser;
import com.milly.auth.domain.model.IssuedRefreshToken;
import com.milly.auth.domain.model.ParsedAccessToken;
import com.milly.auth.domain.model.ParsedRefreshToken;

public interface SessionTokenPort {

    String INVALID_TOKEN_MESSAGE = "Token is invalid.";

    String issueAccessToken(AuthUser user);

    IssuedRefreshToken issueRefreshToken(AuthUser user);

    boolean isValidAccessToken(String token);

    ParsedAccessToken parseAccessToken(String token);

    ParsedRefreshToken parseRefreshToken(String token);
}
