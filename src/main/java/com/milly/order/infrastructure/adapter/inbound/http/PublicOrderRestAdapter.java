package com.milly.order.infrastructure.adapter.inbound.http;

import com.milly.common.web.ApiResponse;
import com.milly.order.application.dto.AddOrderItemsRequest;
import com.milly.order.application.dto.CreateOrderRequest;
import com.milly.order.application.dto.OrderResponse;
import com.milly.order.application.usecase.AddOrderItemsUseCase;
import com.milly.order.application.usecase.CreateOrderUseCase;
import com.milly.order.application.usecase.GetOrderUseCase;
import com.milly.order.application.usecase.ListOrdersUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/tables/{tableId}/orders")
public class PublicOrderRestAdapter {

    private final CreateOrderUseCase createOrderUseCase;
    private final ListOrdersUseCase listOrdersUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final AddOrderItemsUseCase addOrderItemsUseCase;

    public PublicOrderRestAdapter(
            CreateOrderUseCase createOrderUseCase,
            ListOrdersUseCase listOrdersUseCase,
            GetOrderUseCase getOrderUseCase,
            AddOrderItemsUseCase addOrderItemsUseCase) {
        this.createOrderUseCase = createOrderUseCase;
        this.listOrdersUseCase = listOrdersUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.addOrderItemsUseCase = addOrderItemsUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @PathVariable UUID tableId,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = createOrderUseCase.execute(tableId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Order created successfully."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> listOrders(@PathVariable UUID tableId) {
        List<OrderResponse> response = listOrdersUseCase.execute(tableId);
        return ResponseEntity.ok(ApiResponse.success(response, "Orders retrieved successfully."));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable UUID tableId,
            @PathVariable UUID orderId) {
        OrderResponse response = getOrderUseCase.execute(tableId, orderId);
        return ResponseEntity.ok(ApiResponse.success(response, "Order retrieved successfully."));
    }

    @PostMapping("/{orderId}/items")
    public ResponseEntity<ApiResponse<OrderResponse>> addItems(
            @PathVariable UUID tableId,
            @PathVariable UUID orderId,
            @Valid @RequestBody AddOrderItemsRequest request) {
        OrderResponse response = addOrderItemsUseCase.execute(tableId, orderId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Items added successfully."));
    }
}
