package com.producttagger.backend.product.application;

import com.producttagger.backend.catalog.application.CatalogService;
import com.producttagger.backend.catalog.domain.Category;
import com.producttagger.backend.shared.security.AuthenticatedUser;
import com.producttagger.backend.product.domain.Product;
import com.producttagger.backend.product.domain.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ProductRepository products;
    private final CatalogService catalog;
    private final AttributeValidator validator;

    public ReviewService(ProductRepository products, CatalogService catalog, AttributeValidator validator) {
        this.products = products;
        this.catalog = catalog;
        this.validator = validator;
    }

    /**
     * Approves the reviewer's final category and attributes after validating
     * them against the category's active schema.
     */
    @Transactional
    public Product approve(UUID productId, String categoryCode, Map<String, Object> attributes) {
        Map<String, Object> safeAttributes = attributes == null ? Map.of() : attributes;

        Product product = findProduct(productId);

        Category category = catalog.categoryByCode(categoryCode);

        if (!category.isLeaf()) {
            throw new IllegalArgumentException("Category '%s' is not a leaf category".formatted(categoryCode));
        }

        validator.validate(safeAttributes, catalog.activeSchemaOf(category.getId()).attributeDefinitions());

        product.approve(category, safeAttributes, currentReviewerName());

        products.save(product);

        log.info("Product {} approved as '{}'", productId, categoryCode);

        return product;
    }

    @Transactional
    public Product reject(UUID productId) {
        Product product = findProduct(productId);

        product.reject();

        products.save(product);

        log.info("Product {} rejected", productId);

        return product;
    }

    /**
     * Re-enters the tagging pipeline: the domain transition registers the
     * ProductReadyForTagging event, so the outbox/queue flow picks it up again.
     */
    @Transactional
    public Product retag(UUID productId) {
        Product product = findProduct(productId);

        product.requestRetagging();

        products.save(product);

        log.info("Product {} re-queued for tagging", productId);

        return product;
    }

    @Transactional
    public Product updateContent(UUID productId, String titleTr, String titleEn,
                                 String descriptionTr, String descriptionEn) {
        Product product = findProduct(productId);

        product.updateGeneratedContent(titleTr, titleEn, descriptionTr, descriptionEn);

        products.save(product);

        return product;
    }

    @Transactional
    public Product regenerateContent(UUID productId) {
        Product product = findProduct(productId);

        product.requestContentRegeneration();

        products.save(product);

        log.info("Content regeneration requested for product {}", productId);

        return product;
    }

    private String currentReviewerName() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user
                ? user.name()
                : null;
    }

    private Product findProduct(UUID productId) {
        return products.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
