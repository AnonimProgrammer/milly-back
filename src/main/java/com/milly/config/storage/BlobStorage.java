package com.milly.config.storage;

public interface BlobStorage {

    BlobObject upload(String key, byte[] content, String mimeType);

    BlobObject download(String key);

    void delete(String key);
}
