package com.milly.auth.infrastructure.adapter.inbound.http;

import com.milly.auth.application.dto.AdminUserResponse;
import com.milly.auth.application.dto.UpdateAdminUserRequest;
import com.milly.auth.application.usecase.ListUsersUseCase;
import com.milly.auth.application.usecase.UpdateAdminUserUseCase;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.domain.valueobject.UserStatus;
import com.milly.common.application.dto.ApiResponse;
import com.milly.common.application.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
public class AdminUserRestAdapter {

    private final ListUsersUseCase listUsersUseCase;
    private final UpdateAdminUserUseCase updateAdminUserUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> listUsers(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime createdTo) {
        return ResponseEntity.ok(ApiResponse.success(
                listUsersUseCase.execute(
                        cursor,
                        limit,
                        status,
                        role,
                        email,
                        phoneNumber,
                        name,
                        createdFrom,
                        createdTo),
                "Users retrieved successfully."));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUser(
            @PathVariable UUID userId,
            @RequestBody UpdateAdminUserRequest request,
            @AuthenticationPrincipal UUID actorUserId) {
        return ResponseEntity.ok(ApiResponse.success(
                updateAdminUserUseCase.execute(actorUserId, userId, request),
                "User updated successfully."));
    }
}
