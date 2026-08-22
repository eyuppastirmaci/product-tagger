package com.producttagger.backend.product.application;

import java.io.InputStream;

/**
 * Port for product image object storage; the S3/MinIO implementation lives in
 * the infrastructure layer.
 */
public interface ImageStorage {

    void store(String key, InputStream content, long contentLength, String contentType);

    InputStream load(String key);

    void delete(String key);
}
