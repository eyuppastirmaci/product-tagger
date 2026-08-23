package com.producttagger.backend.product.application;

import com.producttagger.backend.product.domain.Product;
import com.producttagger.backend.product.domain.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductUploadService {

    private static final Logger log = LoggerFactory.getLogger(ProductUploadService.class);

    // content type -> file extension of the stored original
    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            MediaType.IMAGE_JPEG_VALUE, "jpg",
            MediaType.IMAGE_PNG_VALUE, "png",
            "image/webp", "webp");

    private final ImageStorage imageStorage;
    private final ImageProcessor imageProcessor;
    private final ProductRepository products;
    private final TransactionTemplate transaction;

    public ProductUploadService(ImageStorage imageStorage,
                                ImageProcessor imageProcessor,
                                ProductRepository products,
                                PlatformTransactionManager transactionManager) {
        this.imageStorage = imageStorage;
        this.imageProcessor = imageProcessor;
        this.products = products;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * Stores the original, derives the processed and thumbnail variants and
     * persists the product as {@code PREPROCESSED}. Processing and storage I/O
     * run outside any transaction (only the final save is transactional), and
     * a failure after the first store deletes the already stored objects so
     * the object storage does not accumulate orphans. Synchronous for now;
     * moves behind the queue in the async slice.
     */
    public Product upload(byte[] content, String contentType) {
        if (content.length == 0) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        String extension = ALLOWED_CONTENT_TYPES.get(contentType);
        if (extension == null) {
            throw new UnsupportedImageTypeException(contentType);
        }

        // Process before any I/O: the most failure-prone step aborts the
        // upload while there is still nothing to clean up
        ImageProcessor.ProcessedImages processed = imageProcessor.process(content);

        // The product id doubles as the storage key prefix
        UUID id = UUID.randomUUID();

        String originalKey = "products/%s/original.%s".formatted(id, extension);
        String processedKey = "products/%s/processed.jpg".formatted(id);
        String thumbnailKey = "products/%s/thumbnail.jpg".formatted(id);

        List<String> storedKeys = new ArrayList<>();

        try {
            imageStorage.store(originalKey, new ByteArrayInputStream(content), content.length, contentType);
            storedKeys.add(originalKey);

            imageStorage.store(processedKey, new ByteArrayInputStream(processed.processed()),
                    processed.processed().length, MediaType.IMAGE_JPEG_VALUE);
            storedKeys.add(processedKey);

            imageStorage.store(thumbnailKey, new ByteArrayInputStream(processed.thumbnail()),
                    processed.thumbnail().length, MediaType.IMAGE_JPEG_VALUE);
            storedKeys.add(thumbnailKey);

            // The save publishes the registered domain events, and the outbox
            // rows they produce commit atomically with the product row
            return transaction.execute(tx -> {
                Product product = Product.upload(id, originalKey);

                product.markPreprocessed(processedKey, thumbnailKey);

                return products.save(product);
            });
        } catch (RuntimeException e) {
            deleteQuietly(storedKeys);

            throw e;
        }
    }

    // Compensation for the missing cross-store transaction: a DB rollback
    // cannot undo MinIO writes, so failed uploads clean up after themselves
    private void deleteQuietly(List<String> keys) {
        for (String key : keys) {
            try {
                imageStorage.delete(key);
            } catch (RuntimeException e) {
                log.warn("Failed to clean up object '{}' of a failed upload", key, e);
            }
        }
    }
}
