package com.milly.table.infrastructure.adapter.inbound.http;

import com.milly.common.web.ApiResponse;
import com.milly.table.application.dto.PublicTableResponse;
import com.milly.table.application.usecase.GetPublicTableUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/tables")
@RequiredArgsConstructor
public class PublicTableRestAdapter {

    private final GetPublicTableUseCase getPublicTableUseCase;

    @GetMapping("/{tableId}")
    public ResponseEntity<ApiResponse<PublicTableResponse>> getTable(@PathVariable UUID tableId) {
        PublicTableResponse table = getPublicTableUseCase.execute(tableId);
        return ResponseEntity.ok(ApiResponse.success(table, "Table retrieved successfully."));
    }
}
