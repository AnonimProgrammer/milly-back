package com.milly.common.infrastructure.adapter.inbound.http;

import com.milly.common.application.dto.ApiResponse;
import com.milly.common.application.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException exception) {
        return HttpErrorResponses.of(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(InvalidInvitationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidInvitation(InvalidInvitationException exception) {
        return HttpErrorResponses.of(
                HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, InvalidInvitationException.MESSAGE);
    }

    @ExceptionHandler(VenueMembershipAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleVenueMembershipAlreadyExists(
            VenueMembershipAlreadyExistsException exception) {
        return HttpErrorResponses.of(
                HttpStatus.CONFLICT, ErrorCode.CONFLICT, VenueMembershipAlreadyExistsException.MESSAGE);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
        return HttpErrorResponses.of(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, AccessDeniedException.MESSAGE);
    }

    @ExceptionHandler(InactiveMembershipException.class)
    public ResponseEntity<ApiResponse<Void>> handleInactiveMembership(InactiveMembershipException exception) {
        return HttpErrorResponses.of(
                HttpStatus.FORBIDDEN, ErrorCode.MEMBERSHIP_INACTIVE, InactiveMembershipException.MESSAGE);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException exception) {
        return HttpErrorResponses.of(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, ResourceNotFoundException.MESSAGE);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStateTransition(InvalidStateTransitionException ignored) {
        return HttpErrorResponses.of(
                HttpStatus.CONFLICT, ErrorCode.CONFLICT, InvalidStateTransitionException.MESSAGE);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleIdempotencyConflict(IdempotencyConflictException ignored) {
        return HttpErrorResponses.of(HttpStatus.CONFLICT, ErrorCode.CONFLICT, IdempotencyConflictException.MESSAGE);
    }

    @ExceptionHandler(PaymentValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentValidation(PaymentValidationException exception) {
        return HttpErrorResponses.of(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedOperation(UnsupportedOperationException exception) {
        return HttpErrorResponses.of(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst().filter(error -> error.getDefaultMessage() != null)
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("Validation failed.");
        return HttpErrorResponses.of(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(AiServiceUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiServiceUnavailable(AiServiceUnavailableException exception) {
        if (exception.getCause() != null) {
                log.warn("AI service unavailable: {}", exception.getCause().getMessage());
        }
        return HttpErrorResponses.of(
                HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        return HttpErrorResponses.of(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, exception.getMessage());
    }
}