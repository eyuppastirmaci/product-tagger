package com.producttagger.backend.product.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.s3")
public record S3StorageProperties(String endpoint, String accessKey, String secretKey, String bucket) {
}
