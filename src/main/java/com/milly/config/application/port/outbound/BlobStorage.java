package com.milly.config.application.port.outbound;

import com.milly.config.domain.model.BlobObject;

public interface BlobStorage {

    BlobObject upload(String key, byte[] content, String mimeType);

    BlobObject download(String key);

    void delete(String key);
}
