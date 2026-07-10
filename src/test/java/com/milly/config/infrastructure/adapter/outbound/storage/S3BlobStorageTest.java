package com.milly.config.infrastructure.adapter.outbound.storage;

import com.milly.config.domain.model.BlobObject;
import com.milly.config.infrastructure.config.storage.StorageProperties;
import com.milly.common.application.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3BlobStorageTest {

    private static final String BUCKET = "milly-assets";
    private static final String REGION = "eu-west-1";
    private static final String KEY = "venues/1/tables/2/qr.png";

    @Mock
    private S3Client s3Client;

    private S3BlobStorage blobStorage;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties(
                new StorageProperties.S3(BUCKET, REGION, "access-key", "secret-key", ""));
        blobStorage = new S3BlobStorage(s3Client, properties);
    }

    @Test
    void uploadStoresObjectAndReturnsPublicUrl() {
        byte[] content = "png-bytes".getBytes(StandardCharsets.UTF_8);

        BlobObject result = blobStorage.upload(KEY, content, "image/png");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo(BUCKET);
        assertThat(request.key()).isEqualTo(KEY);
        assertThat(request.contentType()).isEqualTo("image/png");
        assertThat(result.key()).isEqualTo(KEY);
        assertThat(result.content()).isEqualTo(content);
        assertThat(result.mimeType()).isEqualTo("image/png");
        assertThat(result.url()).isEqualTo("https://milly-assets.s3.eu-west-1.amazonaws.com/" + KEY);
    }

    @Test
    void uploadUsesConfiguredPublicBaseUrlWhenPresent() {
        StorageProperties properties = new StorageProperties(
                new StorageProperties.S3(BUCKET, REGION, "access-key", "secret-key", "https://cdn.example.com/"));
        S3BlobStorage storage = new S3BlobStorage(s3Client, properties);

        BlobObject result = storage.upload(KEY, new byte[] {1}, "image/png");

        assertThat(result.url()).isEqualTo("https://cdn.example.com/" + KEY);
    }

    @Test
    void downloadReturnsBlobObject() throws Exception {
        byte[] content = "png-bytes".getBytes(StandardCharsets.UTF_8);
        GetObjectResponse response = GetObjectResponse.builder()
                .contentType("image/png")
                .build();
        ResponseInputStream<GetObjectResponse> responseStream = new ResponseInputStream<>(
                response,
                AbortableInputStream.create(new ByteArrayInputStream(content)));

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);

        BlobObject result = blobStorage.download(KEY);

        assertThat(result.key()).isEqualTo(KEY);
        assertThat(result.content()).isEqualTo(content);
        assertThat(result.mimeType()).isEqualTo("image/png");
        assertThat(result.url()).isEqualTo("https://milly-assets.s3.eu-west-1.amazonaws.com/" + KEY);
    }

    @Test
    void downloadThrowsNotFoundWhenObjectMissing() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());

        assertThatThrownBy(() -> blobStorage.download(KEY))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesObjectFromBucket() {
        blobStorage.delete(KEY);

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());

        DeleteObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo(BUCKET);
        assertThat(request.key()).isEqualTo(KEY);
    }
}
