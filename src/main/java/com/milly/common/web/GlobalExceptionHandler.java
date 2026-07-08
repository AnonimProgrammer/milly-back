package com.milly.common.web;

import com.milly.common.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "Validation failed." : error.getDefaultMessage())
                .orElse("Validation failed.");
        return HttpErrorResponses.of(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        return HttpErrorResponses.of(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, exception.getMessage());
    }
}
