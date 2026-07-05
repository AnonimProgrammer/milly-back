package com.milly.table.infrastructure.adapter.inbound.http;

import com.milly.common.web.ApiResponse;
import com.milly.table.application.dto.CreateTableRequest;
import com.milly.table.application.dto.TableResponse;
import com.milly.table.application.dto.UpdateTableLabelRequest;
import com.milly.table.application.dto.TableQrResponse;
import com.milly.table.application.usecase.CreateTableUseCase;
import com.milly.table.application.usecase.DeactivateTableUseCase;
import com.milly.table.application.usecase.GenerateTableQrUseCase;
import com.milly.table.application.usecase.GetTableUseCase;
import com.milly.table.application.usecase.ListTablesUseCase;
import com.milly.table.application.usecase.UpdateTableLabelUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/v1/venues/{venueId}/tables")
@RequiredArgsConstructor
public class TableRestAdapter {

    private final ListTablesUseCase listTablesUseCase;
    private final CreateTableUseCase createTableUseCase;
    private final GetTableUseCase getTableUseCase;
    private final UpdateTableLabelUseCase updateTableLabelUseCase;
    private final DeactivateTableUseCase deactivateTableUseCase;
    private final GenerateTableQrUseCase generateTableQrUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TableResponse>>> listTables(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID venueId) {
        List<TableResponse> tables = listTablesUseCase.execute(userId, venueId);
        return ResponseEntity.ok(ApiResponse.success(tables, "Tables retrieved successfully."));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TableResponse>> createTable(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID venueId,
            @Valid @RequestBody CreateTableRequest request) {
        TableResponse table = createTableUseCase.execute(userId, venueId, request);
        return ResponseEntity.status(201).body(ApiResponse.created(table, "Table created successfully."));
    }

    @GetMapping("/{tableId}")
    public ResponseEntity<ApiResponse<TableResponse>> getTable(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID venueId,
            @PathVariable UUID tableId) {
        TableResponse table = getTableUseCase.execute(userId, venueId, tableId);
        return ResponseEntity.ok(ApiResponse.success(table, "Table retrieved successfully."));
    }

    @PatchMapping("/{tableId}")
    public ResponseEntity<ApiResponse<TableResponse>> updateTableLabel(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID venueId,
            @PathVariable UUID tableId,
            @Valid @RequestBody UpdateTableLabelRequest request) {
        TableResponse table = updateTableLabelUseCase.execute(userId, venueId, tableId, request);
        return ResponseEntity.ok(ApiResponse.success(table, "Table updated successfully."));
    }

    @PostMapping("/{tableId}/deactivate")
    public ResponseEntity<Void> deactivateTable(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID venueId,
            @PathVariable UUID tableId) {
        deactivateTableUseCase.execute(userId, venueId, tableId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tableId}/qr")
    public ResponseEntity<ApiResponse<TableQrResponse>> generateTableQr(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID venueId,
            @PathVariable UUID tableId) {
        TableQrResponse qr = generateTableQrUseCase.execute(userId, venueId, tableId);
        return ResponseEntity.ok(ApiResponse.success(qr, "Table QR generated successfully."));
    }
}
