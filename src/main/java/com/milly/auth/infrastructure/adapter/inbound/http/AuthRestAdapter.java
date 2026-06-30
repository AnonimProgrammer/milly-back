package com.milly.auth.infrastructure.adapter.inbound.http;

import com.milly.auth.application.dto.ContinueAuthRequest;
import com.milly.auth.application.dto.ContinueAuthResponse;
import com.milly.auth.application.usecase.ContinueAuthUseCase;
import com.milly.auth.infrastructure.adapter.outbound.security.AuthCookieWriter;
import com.milly.common.web.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthRestAdapter {

    private final ContinueAuthUseCase continueAuthUseCase;
    private final boolean secureCookies;

    public AuthRestAdapter(
            ContinueAuthUseCase continueAuthUseCase,
            @Value("${auth.cookies.secure:false}") boolean secureCookies) {
        this.continueAuthUseCase = continueAuthUseCase;
        this.secureCookies = secureCookies;
    }

    @PostMapping("/continue")
    public ResponseEntity<ApiResponse<ContinueAuthResponseBody>> continueAuth(
            @Valid @RequestBody ContinueAuthRequest request,
            HttpServletResponse response) {
        ContinueAuthResponse result = continueAuthUseCase.execute(request);
        AuthCookieWriter.writeAuthCookies(response, result.accessToken(), result.refreshToken(), secureCookies);
        return ResponseEntity.ok(ApiResponse.success(
                new ContinueAuthResponseBody(result.newUser()),
                "Authentication successful."));
    }

    public record ContinueAuthResponseBody(boolean newUser) {
    }
}
