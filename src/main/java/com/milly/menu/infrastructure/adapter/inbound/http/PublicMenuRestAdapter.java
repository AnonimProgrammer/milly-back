package com.milly.menu.infrastructure.adapter.inbound.http;

import com.milly.common.web.ApiResponse;
import com.milly.menu.application.dto.MenuItemResponse;
import com.milly.menu.application.usecase.ListPublicMenuItemsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/tables/{tableId}/menu")
@RequiredArgsConstructor
public class PublicMenuRestAdapter {

    private final ListPublicMenuItemsUseCase listPublicMenuItemsUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> listMenuItems(@PathVariable UUID tableId) {
        List<MenuItemResponse> response = listPublicMenuItemsUseCase.execute(tableId);
        return ResponseEntity.ok(ApiResponse.success(response, "Menu items retrieved successfully."));
    }
}
