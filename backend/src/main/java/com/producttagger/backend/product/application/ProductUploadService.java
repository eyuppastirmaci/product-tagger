package com.producttagger.backend.product.application;

import com.producttagger.backend.product.domain.Product;
import com.producttagger.backend.product.domain.ProductRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductUploadService {

    // content type -> file extension of the stored original
    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            MediaType.IMAGE_JPEG_VALUE, "jpg",
            MediaType.IMAGE_PNG_VALUE, "png",
            "image/webp", "webp");

    private final ImageStorage imageStorage;
    private final ImageProcessor imageProcessor;
    private final ProductRepository products;

    public ProductUploadService(ImageStorage imageStorage,
                                ImageProcessor imageProcessor,
                                ProductRepository products) {
        this.imageStorage = imageStorage;
        this.imageProcessor = imageProcessor;
        this.products = products;
    }

    /**
     * Stores the original, derives the processed and thumbnail variants and
     * persists the product as {@code PREPROCESSED}. Synchronous for now; moves
     * behind the queue in the async slice.
     */
    @Transactional
    public Product upload(byte[] content, String contentType) {
        if (content.length == 0) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        String extension = ALLOWED_CONTENT_TYPES.get(contentType);
        if (extension == null) {
            throw new UnsupportedImageTypeException(contentType);
        }

        // The product id doubles as the storage key prefix
        UUID id = UUID.randomUUID();

        String originalKey = "products/%s/original.%s".formatted(id, extension);
        String processedKey = "products/%s/processed.jpg".formatted(id);
        String thumbnailKey = "products/%s/thumbnail.jpg".formatted(id);

        imageStorage.store(originalKey, new ByteArrayInputStream(content), content.length, contentType);

        ImageProcessor.ProcessedImages processed = imageProcessor.process(content);

        imageStorage.store(processedKey, new ByteArrayInputStream(processed.processed()),
                processed.processed().length, MediaType.IMAGE_JPEG_VALUE);

        imageStorage.store(thumbnailKey, new ByteArrayInputStream(processed.thumbnail()),
                processed.thumbnail().length, MediaType.IMAGE_JPEG_VALUE);

        Product product = Product.upload(id, originalKey);

        product.markPreprocessed(processedKey, thumbnailKey);

        return products.save(product);
    }
}
