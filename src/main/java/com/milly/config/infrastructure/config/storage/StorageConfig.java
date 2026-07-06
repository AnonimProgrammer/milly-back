package com.milly.config.infrastructure.config.storage;

import com.milly.config.application.port.outbound.BlobStorage;
import com.milly.config.infrastructure.adapter.outbound.storage.InMemoryBlobStorage;
import com.milly.config.infrastructure.adapter.outbound.storage.S3BlobStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "storage.s3", name = "bucket")
    S3Client s3Client(StorageProperties properties) {
        StorageProperties.S3 s3 = properties.s3();
        var builder = S3Client.builder().region(Region.of(s3.region()));

        if (StringUtils.hasText(s3.accessKeyId()) && StringUtils.hasText(s3.secretAccessKey())) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(s3.accessKeyId(), s3.secretAccessKey());
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.builder().build());
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "storage.s3", name = "bucket")
    BlobStorage s3BlobStorage(S3Client s3Client, StorageProperties properties) {
        return new S3BlobStorage(s3Client, properties);
    }

    @Bean
    @ConditionalOnMissingBean(BlobStorage.class)
    BlobStorage inMemoryBlobStorage() {
        return new InMemoryBlobStorage();
    }
}
