package com.milly.common.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class HttpErrorResponses {

    private HttpErrorResponses() {
    }

    public static ResponseEntity<ApiResponse<Void>> of(HttpStatus status, ErrorCode errorCode, String message) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(status.value(), message, errorCode.name()));
    }
}
