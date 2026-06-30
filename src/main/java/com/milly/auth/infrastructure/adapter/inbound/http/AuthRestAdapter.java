package com.milly.auth.infrastructure.adapter.inbound.http;

import com.milly.auth.application.dto.ContinueAuthRequest;
import com.milly.auth.application.dto.ContinueAuthResponse;
import com.milly.auth.application.dto.ContinueAuthResponseBody;
import com.milly.auth.application.dto.CurrentUserResponse;

import com.milly.auth.application.usecase.ContinueAuthUseCase;
import com.milly.auth.application.usecase.GetCurrentUserUseCase;
import com.milly.auth.application.dto.RefreshSessionResponse;
import com.milly.auth.application.usecase.LogoutUseCase;
import com.milly.auth.application.usecase.RefreshSessionUseCase;
import com.milly.auth.infrastructure.adapter.outbound.security.AuthCookieWriter;
import com.milly.common.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthRestAdapter {

    private final ContinueAuthUseCase continueAuthUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshSessionUseCase refreshSessionUseCase;

    @Value("${auth.cookies.secure:false}")
    private boolean secureCookies;

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

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getCurrentUser(
            @AuthenticationPrincipal UUID userId) {
        CurrentUserResponse response = getCurrentUserUseCase.execute(userId);
        return ResponseEntity.ok(
                ApiResponse.success(
                        response,
                        "Current user retrieved successfully."
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = AuthCookieWriter.readCookie(request, AuthCookieWriter.REFRESH_TOKEN_COOKIE)
                .orElse(null);
        logoutUseCase.execute(refreshToken);
        AuthCookieWriter.clearAuthCookies(response, secureCookies);
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refreshSession(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = AuthCookieWriter.readCookie(request, AuthCookieWriter.REFRESH_TOKEN_COOKIE)
                .orElse(null);
        RefreshSessionResponse result = refreshSessionUseCase.execute(refreshToken);
        AuthCookieWriter.writeAuthCookies(
                response, result.accessToken(), result.refreshToken(), secureCookies);
        return ResponseEntity.ok(ApiResponse.success(null, "Session refreshed."));
    }
}
