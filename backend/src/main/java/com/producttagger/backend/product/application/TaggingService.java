package com.producttagger.backend.product.application;

import com.producttagger.backend.product.domain.Product;
import com.producttagger.backend.product.domain.ProductRepository;
import com.producttagger.backend.product.domain.ProductStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TaggingService {

    private static final Logger log = LoggerFactory.getLogger(TaggingService.class);

    private final ProductRepository products;

    public TaggingService(ProductRepository products) {
        this.products = products;
    }

    /**
     * Skeleton of the tagging pipeline: moves the product to {@code TAGGING}.
     * The AI slice (category descent + attribute extraction) plugs in here.
     */
    @Transactional
    public void startTagging(UUID productId) {
        Product product = products.findById(productId).orElse(null);

        if (product == null) {
            log.warn("Ignoring tagging request for unknown product {}", productId);
            return;
        }

        // At-least-once delivery: a redelivered message finds the product already
        // past PREPROCESSED — skip instead of failing into the DLQ
        if (product.getStatus() != ProductStatus.PREPROCESSED) {
            log.info("Ignoring tagging request for product {} in status {}", productId, product.getStatus());
            return;
        }

        product.startTagging();

        products.save(product);

        log.info("Product {} moved to TAGGING", productId);
    }
}
