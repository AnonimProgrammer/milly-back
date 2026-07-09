package com.milly.order.infrastructure.adapter.inbound.http;

import com.milly.common.web.ApiResponse;
import com.milly.common.web.PageResponse;
import com.milly.order.application.dto.StaffOrderResponse;
import com.milly.order.application.dto.OrderPreparationEstimateResponse;
import com.milly.order.application.usecase.ApproveOrderUseCase;
import com.milly.order.application.usecase.CloseOrderUseCase;
import com.milly.order.application.usecase.EstimateOrderPreparationTimeUseCase;
import com.milly.order.application.usecase.GetVenueOrderUseCase;
import com.milly.order.application.usecase.ListVenueOrdersUseCase;
import com.milly.order.application.usecase.RejectOrderUseCase;
import com.milly.order.domain.valueobject.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/venues/{venueId}/orders")
public class StaffOrderRestAdapter {

    private final ListVenueOrdersUseCase listVenueOrdersUseCase;
    private final GetVenueOrderUseCase getVenueOrderUseCase;
    private final ApproveOrderUseCase approveOrderUseCase;
    private final RejectOrderUseCase rejectOrderUseCase;
    private final CloseOrderUseCase closeOrderUseCase;
    private final EstimateOrderPreparationTimeUseCase estimateOrderPreparationTimeUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StaffOrderResponse>>> listOrders(
            @PathVariable UUID venueId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(
                listVenueOrdersUseCase.execute(venueId, userId, status, from, to, cursor, limit),
                "Orders retrieved successfully."));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<StaffOrderResponse>> getOrder(
            @PathVariable UUID venueId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(
                getVenueOrderUseCase.execute(venueId, userId, orderId),
                "Order retrieved successfully."));
    }

    @PostMapping("/{orderId}/approve")
    public ResponseEntity<ApiResponse<StaffOrderResponse>> approveOrder(
            @PathVariable UUID venueId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(
                approveOrderUseCase.execute(venueId, userId, orderId),
                "Order approved successfully."));
    }

    @PostMapping("/{orderId}/reject")
    public ResponseEntity<ApiResponse<StaffOrderResponse>> rejectOrder(
            @PathVariable UUID venueId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(
                rejectOrderUseCase.execute(venueId, userId, orderId),
                "Order rejected successfully."));
    }

    @PostMapping("/{orderId}/close")
    public ResponseEntity<ApiResponse<StaffOrderResponse>> closeOrder(
            @PathVariable UUID venueId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(
                closeOrderUseCase.execute(venueId, userId, orderId),
                "Order closed successfully."));
    }

    @PostMapping("/{orderId}/preparation-time/estimate")
    public ResponseEntity<ApiResponse<OrderPreparationEstimateResponse>> estimatePreparationTime(
            @PathVariable UUID venueId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(
                estimateOrderPreparationTimeUseCase.execute(venueId, userId, orderId),
                "Order preparation time estimated successfully."));
    }
}
