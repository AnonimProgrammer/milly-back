package com.milly.config.domain.model;

public record BlobObject(
        String key,
        byte[] content,
        String mimeType,
        String url
) {
    public static BlobObject forUpload(String key, byte[] content, String mimeType) {
        return new BlobObject(key, content, mimeType, null);
    }

    public BlobObject withUrl(String url) {
        return new BlobObject(key, content, mimeType, url);
    }
}
