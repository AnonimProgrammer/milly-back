package com.milly.config.storage;

import com.milly.common.exception.ResourceNotFoundException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBlobStorage implements BlobStorage {

    private final Map<String, BlobObject> objects = new ConcurrentHashMap<>();

    @Override
    public BlobObject upload(String key, byte[] content, String mimeType) {
        BlobObject blob = new BlobObject(key, content, mimeType, "https://storage.local/" + key);
        objects.put(key, blob);
        return blob;
    }

    @Override
    public BlobObject download(String key) {
        BlobObject blob = objects.get(key);
        if (blob == null) {
            throw new ResourceNotFoundException();
        }
        return blob;
    }

    @Override
    public void delete(String key) {
        objects.remove(key);
    }
}
