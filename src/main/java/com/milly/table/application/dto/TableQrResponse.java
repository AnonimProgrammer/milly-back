package com.milly.table.application.dto;

import java.util.UUID;

public record TableQrResponse(
        UUID tableId,
        String customerUrl,
        String qrImageUrl
) {
    public static TableQrResponse of(UUID tableId, String customerUrl, String qrImageUrl) {
        return new TableQrResponse(tableId, customerUrl, qrImageUrl);
    }
}
