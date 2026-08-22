package com.producttagger.backend.product.infrastructure.storage;

import com.producttagger.backend.product.application.ImageStorage;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

@Component
class S3ImageStorage implements ImageStorage {

    private final S3Client s3;

    private final S3StorageProperties properties;

    S3ImageStorage(S3Client s3, S3StorageProperties properties) {
        this.s3 = s3;
        this.properties = properties;
    }

    /**
     * Creates the bucket on startup if it does not exist yet, so a fresh MinIO
     * volume works without any manual setup.
     */
    @PostConstruct
    void ensureBucketExists() {
        try {
            s3.headBucket(request -> request.bucket(properties.bucket()));
        } catch (NoSuchBucketException e) {
            s3.createBucket(request -> request.bucket(properties.bucket()));
        }
    }

    @Override
    public void store(String key, InputStream content, long contentLength, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .contentType(contentType)
                .build();

        s3.putObject(request, RequestBody.fromInputStream(content, contentLength));
    }

    @Override
    public InputStream load(String key) {
        return s3.getObject(request -> request.bucket(properties.bucket()).key(key));
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(request -> request.bucket(properties.bucket()).key(key));
    }
}
