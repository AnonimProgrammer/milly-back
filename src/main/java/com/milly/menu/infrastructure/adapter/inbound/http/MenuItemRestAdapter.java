package com.milly.menu.infrastructure.adapter.inbound.http;

import com.milly.common.application.dto.ApiResponse;
import com.milly.menu.application.dto.CreateMenuItemRequest;
import com.milly.menu.application.dto.MenuItemResponse;
import com.milly.menu.application.dto.UpdateMenuItemRequest;
import com.milly.menu.application.usecase.CreateMenuItemUseCase;
import com.milly.menu.application.usecase.DeleteMenuItemUseCase;
import com.milly.menu.application.usecase.GetMenuItemUseCase;
import com.milly.menu.application.usecase.ListMenuItemsUseCase;
import com.milly.menu.application.usecase.UpdateMenuItemUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues/{venueId}/menu/items")
@RequiredArgsConstructor
public class MenuItemRestAdapter {

    private final ListMenuItemsUseCase listMenuItemsUseCase;
    private final CreateMenuItemUseCase createMenuItemUseCase;
    private final GetMenuItemUseCase getMenuItemUseCase;
    private final UpdateMenuItemUseCase updateMenuItemUseCase;
    private final DeleteMenuItemUseCase deleteMenuItemUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> listMenuItems(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID venueId) {
        List<MenuItemResponse> menuItems = listMenuItemsUseCase.execute(userId, venueId);
        return ResponseEntity.ok(ApiResponse.success(menuItems, "Menu items retrieved successfully."));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MenuItemResponse>> createMenuItem(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID venueId,
            @Valid @RequestBody CreateMenuItemRequest request) {
        MenuItemResponse menuItem = createMenuItemUseCase.execute(userId, venueId, request);
        return ResponseEntity.status(201).body(ApiResponse.created(menuItem, "Menu item created successfully."));
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getMenuItem(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID venueId,
            @PathVariable UUID itemId) {
        MenuItemResponse menuItem = getMenuItemUseCase.execute(userId, venueId, itemId);
        return ResponseEntity.ok(ApiResponse.success(menuItem, "Menu item retrieved successfully."));
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateMenuItem(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID venueId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateMenuItemRequest request) {
        MenuItemResponse menuItem = updateMenuItemUseCase.execute(userId, venueId, itemId, request);
        return ResponseEntity.ok(ApiResponse.success(menuItem, "Menu item updated successfully."));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteMenuItem(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID venueId,
            @PathVariable UUID itemId) {
        deleteMenuItemUseCase.execute(userId, venueId, itemId);
        return ResponseEntity.noContent().build();
    }
}
