package com.milly.config.infrastructure.adapter.outbound.storage;

import com.milly.config.application.port.outbound.BlobStorage;
import com.milly.config.domain.model.BlobObject;
import com.milly.config.infrastructure.config.storage.StorageProperties;
import com.milly.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.UncheckedIOException;

@RequiredArgsConstructor
public class S3BlobStorage implements BlobStorage {

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    @Override
    public BlobObject upload(String key, byte[] content, String mimeType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket())
                .key(key)
                .contentType(mimeType)
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromBytes(content));
            return BlobObject.forUpload(key, content, mimeType).withUrl(resolvePublicUrl(key));
        } catch (S3Exception exception) {
            throw new IllegalStateException("Failed to upload object to S3.", exception);
        }
    }

    @Override
    public BlobObject download(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket())
                .key(key)
                .build();

        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request)) {
            byte[] content = response.readAllBytes();
            String mimeType = response.response().contentType();
            return new BlobObject(key, content, mimeType, resolvePublicUrl(key));
        } catch (NoSuchKeyException exception) {
            throw new ResourceNotFoundException();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read object from S3.", exception);
        } catch (S3Exception exception) {
            throw new IllegalStateException("Failed to download object from S3.", exception);
        }
    }

    @Override
    public void delete(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket())
                .key(key)
                .build();

        try {
            s3Client.deleteObject(request);
        } catch (S3Exception exception) {
            throw new IllegalStateException("Failed to delete object from S3.", exception);
        }
    }

    String resolvePublicUrl(String key) {
        String publicBaseUrl = storageProperties.s3().publicBaseUrl();
        if (StringUtils.hasText(publicBaseUrl)) {
            String normalizedBaseUrl = publicBaseUrl.stripTrailing();
            if (normalizedBaseUrl.endsWith("/")) {
                normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
            }
            return normalizedBaseUrl + "/" + key;
        }

        return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket(), region(), key);
    }

    private String bucket() {
        return storageProperties.s3().bucket();
    }

    private String region() {
        return storageProperties.s3().region();
    }
}
