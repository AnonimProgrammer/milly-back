package com.milly.billing.domain.model;

public record GeneratedReceipt(
        byte[] content,
        String mimeType,
        String fileExtension
) {}
