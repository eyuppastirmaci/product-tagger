package com.producttagger.backend.product.application;

import com.producttagger.backend.catalog.application.CatalogService;
import com.producttagger.backend.catalog.domain.Category;
import com.producttagger.backend.product.application.TaggingModelClient.AttributeExtraction;
import com.producttagger.backend.product.application.TaggingModelClient.CategoryChoice;
import com.producttagger.backend.product.application.TaggingModelClient.CategoryOption;
import com.producttagger.backend.product.domain.Product;
import com.producttagger.backend.product.domain.ProductRepository;
import com.producttagger.backend.product.domain.ProductStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TaggingService {

    private static final Logger log = LoggerFactory.getLogger(TaggingService.class);

    private final ProductRepository products;

    private final CatalogService catalog;

    private final TaggingModelClient model;

    private final ImageStorage imageStorage;

    private final TransactionTemplate transaction;

    public TaggingService(ProductRepository products,
                          CatalogService catalog,
                          TaggingModelClient model,
                          ImageStorage imageStorage,
                          PlatformTransactionManager transactionManager) {
        this.products = products;
        this.catalog = catalog;
        this.model = model;
        this.imageStorage = imageStorage;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * Runs the tagging pipeline for one product: category descent, attribute
     * extraction and the AI revision. The slow model calls run outside any
     * transaction so DB connections are not held for seconds.
     */
    public void tagProduct(UUID productId) {
        Product product = transaction.execute(tx -> prepareForTagging(productId));

        if (product == null) {
            return;
        }

        byte[] image = loadProcessedImage(product);

        Descent descent = descendCategoryTree(image);

        AttributeExtraction extraction = descent.leaf() == null
                ? null
                : model.extractAttributes(image, catalog.activeSchemaOf(descent.leaf().getId()).attributeDefinitions());

        transaction.executeWithoutResult(tx -> recordProposal(productId, descent, extraction));
    }

    /**
     * Called for dead-lettered messages: all retries are exhausted, so the
     * product falls back to manual tagging via the review queue.
     */
    @Transactional
    public void markTaggingFailed(UUID productId) {
        Product product = products.findById(productId).orElse(null);

        if (product == null) {
            log.warn("Ignoring dead-lettered message for unknown product {}", productId);
            return;
        }

        // Only fail products still stuck in the pipeline; a product that made it
        // to review or approval keeps its state
        if (product.getStatus() != ProductStatus.PREPROCESSED && product.getStatus() != ProductStatus.TAGGING) {
            log.info("Ignoring dead-lettered message for product {} in status {}", productId, product.getStatus());
            return;
        }

        product.markFailed();

        products.save(product);

        log.warn("Product {} marked FAILED after exhausting tagging retries", productId);
    }

    private Product prepareForTagging(UUID productId) {
        Product product = products.findById(productId).orElse(null);

        if (product == null) {
            log.warn("Ignoring tagging request for unknown product {}", productId);
            return null;
        }

        // TAGGING is accepted too: a retried or redelivered message must be able
        // to resume a run that failed mid-pipeline
        if (product.getStatus() == ProductStatus.PREPROCESSED) {
            product.startTagging();
            products.save(product);
        } else if (product.getStatus() != ProductStatus.TAGGING) {
            log.info("Ignoring tagging request for product {} in status {}", productId, product.getStatus());
            return null;
        }

        return product;
    }

    private byte[] loadProcessedImage(Product product) {
        try (InputStream in = imageStorage.load(product.getImagePaths().getProcessed())) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load processed image of product " + product.getId(), e);
        }
    }

    /**
     * Walks the category tree level by level; each step offers only the current
     * node's children plus "other". Stops at a leaf, or with no result when the
     * model picks "other" or answers outside the offered options.
     */
    private Descent descendCategoryTree(byte[] image) {
        Map<String, Double> confidences = new LinkedHashMap<>();
        List<Category> options = catalog.rootCategories();
        Category current = null;
        String modelName = null;

        while (!options.isEmpty()) {
            CategoryChoice choice = model.pickCategory(image, toOptions(options));

            confidences.put("category." + (current == null ? "root" : current.getCode()), choice.confidence());
            modelName = choice.modelName();

            // An answer outside the offered codes counts as "other": never trust free text
            Category selected = choice.isOther() ? null : findByCode(options, choice.code());

            if (selected == null) {
                return new Descent(null, confidences, modelName);
            }

            if (selected.isLeaf()) {
                return new Descent(selected, confidences, modelName);
            }

            current = selected;
            options = catalog.childrenOf(selected.getId());
        }

        // A non-leaf node without children is a tree gap; leave the decision to review
        return new Descent(null, confidences, modelName);
    }

    private void recordProposal(UUID productId, Descent descent, AttributeExtraction extraction) {
        Product product = products.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));

        if (product.getStatus() != ProductStatus.TAGGING) {
            log.info("Skipping proposal for product {} in status {}", productId, product.getStatus());
            return;
        }

        Map<String, Object> confidences = new LinkedHashMap<>(descent.levelConfidences());
        Map<String, Object> attributes = null;
        String modelName = descent.modelName();

        if (extraction != null) {
            attributes = extraction.attributes();
            confidences.putAll(extraction.confidences());
            modelName = extraction.modelName();
        }

        product.proposeTagging(descent.leaf(), attributes, confidences, modelName);

        products.save(product);

        log.info("Product {} tagged as '{}' and moved to PENDING_REVIEW", productId,
                descent.leaf() == null ? "other" : descent.leaf().getCode());
    }

    private List<CategoryOption> toOptions(List<Category> categories) {
        return categories.stream()
                .map(category -> new CategoryOption(category.getCode(), category.getNameEn()))
                .toList();
    }

    private Category findByCode(List<Category> options, String code) {
        return options.stream()
                .filter(category -> category.getCode().equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }

    private record Descent(Category leaf, Map<String, Double> levelConfidences, String modelName) {
    }
}
