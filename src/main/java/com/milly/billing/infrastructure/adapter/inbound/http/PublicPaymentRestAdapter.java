package com.milly.billing.infrastructure.adapter.inbound.http;

import com.milly.billing.application.dto.BillSummaryResponse;
import com.milly.billing.application.dto.CreatePaymentRequest;
import com.milly.billing.application.dto.ProcessPaymentResponse;
import com.milly.billing.application.usecase.GetBillUseCase;
import com.milly.billing.application.usecase.ProcessPaymentUseCase;
import com.milly.common.idempotency.Idempotent;
import com.milly.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/tables/{tableId}/orders/{orderId}")
@RequiredArgsConstructor
public class PublicPaymentRestAdapter {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final GetBillUseCase getBillUseCase;

    @Idempotent
    @PostMapping("/payments")
    public ResponseEntity<ApiResponse<ProcessPaymentResponse>> processPayment(
            @PathVariable UUID tableId,
            @PathVariable UUID orderId,
            @Valid @RequestBody CreatePaymentRequest request) {
        ProcessPaymentResponse response = processPaymentUseCase.execute(tableId, orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Payment processed successfully."));
    }
    @GetMapping("/bill")
    public ResponseEntity<ApiResponse<BillSummaryResponse>> getBill(
            @PathVariable UUID tableId,
            @PathVariable UUID orderId) {
        BillSummaryResponse response = getBillUseCase.execute(tableId, orderId);
        return ResponseEntity.ok(ApiResponse.success(response, "Bill retrieved successfully."));
    }
}