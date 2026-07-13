package com.milly.auth.infrastructure.adapter.inbound.http;

import com.milly.auth.application.exception.RefreshSessionFailedException;
import com.milly.auth.infrastructure.adapter.outbound.security.AuthCookieWriter;
import com.milly.common.application.dto.ApiResponse;
import com.milly.common.application.exception.UserAccountInactiveException;
import com.milly.common.infrastructure.adapter.inbound.http.ErrorCode;
import com.milly.common.infrastructure.adapter.inbound.http.HttpErrorResponses;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthRestAdapter.class)
public class AuthExceptionHandler {

    @Value("${auth.cookies.secure:false}")
    private boolean secureCookies;

    @ExceptionHandler(RefreshSessionFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleRefreshSessionFailed(
            RefreshSessionFailedException exception,
            HttpServletResponse response) {
        AuthCookieWriter.clearAuthCookies(response, secureCookies);
        return HttpErrorResponses.of(
                HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, RefreshSessionFailedException.MESSAGE);
    }

    @ExceptionHandler(UserAccountInactiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAccountInactive(
            UserAccountInactiveException exception,
            HttpServletResponse response) {
        AuthCookieWriter.clearAuthCookies(response, secureCookies);
        return HttpErrorResponses.of(
                HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_INACTIVE, UserAccountInactiveException.MESSAGE);
    }
}
