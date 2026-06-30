package com.milly.auth.infrastructure.adapter.inbound.http;

import com.milly.auth.application.exception.RefreshSessionFailedException;
import com.milly.auth.infrastructure.adapter.outbound.security.AuthCookieWriter;
import com.milly.common.web.ApiResponse;
import com.milly.common.web.ErrorCode;
import com.milly.common.web.HttpErrorResponses;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthRestAdapter.class)
public class AuthExceptionHandler {

    private final boolean secureCookies;

    public AuthExceptionHandler(@Value("${auth.cookies.secure:false}") boolean secureCookies) {
        this.secureCookies = secureCookies;
    }

    @ExceptionHandler(RefreshSessionFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleRefreshSessionFailed(
            RefreshSessionFailedException exception,
            HttpServletResponse response) {
        AuthCookieWriter.clearAuthCookies(response, secureCookies);
        return HttpErrorResponses.of(
                HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, RefreshSessionFailedException.MESSAGE);
    }
}
