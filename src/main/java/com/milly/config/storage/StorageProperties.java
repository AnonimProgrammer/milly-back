package com.milly.config.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
        @DefaultValue S3 s3
) {

    public record S3(
            String bucket,
            String region,
            String accessKeyId,
            String secretAccessKey,
            String publicBaseUrl
    ) {
        public S3 {
            if (region == null || region.isBlank()) {
                region = "eu-west-1";
            }
        }
    }
}
