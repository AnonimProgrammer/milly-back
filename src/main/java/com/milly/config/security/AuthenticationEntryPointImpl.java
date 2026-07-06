package com.milly.config.security;

import org.jspecify.annotations.NonNull;
import tools.jackson.databind.ObjectMapper;
import com.milly.common.web.ApiResponse;
import com.milly.common.web.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    private static final String DEFAULT_MESSAGE = "Authentication is required to access this resource.";

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull AuthenticationException authException) throws IOException {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        Object requestMessage = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_MESSAGE_ATTRIBUTE);
        String message = requestMessage instanceof String errorMessage ? errorMessage : DEFAULT_MESSAGE;

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.error(status.value(), message, ErrorCode.UNAUTHORIZED.name()));
    }
}
