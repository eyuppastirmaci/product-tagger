package com.producttagger.backend.product.application;

import com.producttagger.backend.catalog.domain.Category;
import com.producttagger.backend.product.application.DescriptionModelClient.Descriptions;
import com.producttagger.backend.product.domain.Product;
import com.producttagger.backend.product.domain.ProductRepository;
import com.producttagger.backend.product.domain.ProductStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;

@Service
public class DescriptionService {

    private static final Logger log = LoggerFactory.getLogger(DescriptionService.class);

    private final ProductRepository products;

    private final DescriptionModelClient model;

    private final TransactionTemplate transaction;

    public DescriptionService(ProductRepository products,
                              DescriptionModelClient model,
                              PlatformTransactionManager transactionManager) {
        this.products = products;
        this.model = model;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * Generates the bilingual descriptions for an approved product; the model
     * call runs outside any transaction, like the tagging pipeline.
     */
    public void generateFor(UUID productId) {
        GenerationInput input = transaction.execute(tx -> loadInput(productId));

        if (input == null) {
            return;
        }

        Descriptions descriptions = model.generate(input.categoryNameEn(), input.categoryNameTr(), input.attributes());

        transaction.executeWithoutResult(tx -> attach(productId, descriptions));
    }

    private GenerationInput loadInput(UUID productId) {
        Product product = products.findById(productId).orElse(null);

        if (product == null) {
            log.warn("Ignoring description request for unknown product {}", productId);
            return null;
        }

        // A redelivered message finds the descriptions already attached: skip
        if (product.getStatus() != ProductStatus.APPROVED || product.getDescriptionTr() != null) {
            log.info("Ignoring description request for product {} in status {}", productId, product.getStatus());
            return null;
        }

        Category category = product.getCategory();

        return new GenerationInput(category.getNameEn(), category.getNameTr(), product.getAttributes());
    }

    private void attach(UUID productId, Descriptions descriptions) {
        Product product = products.findById(productId).orElse(null);

        if (product == null || product.getStatus() != ProductStatus.APPROVED || product.getDescriptionTr() != null) {
            return;
        }

        product.attachDescriptions(descriptions.tr(), descriptions.en());

        products.save(product);

        log.info("Descriptions generated for product {}", productId);
    }

    private record GenerationInput(String categoryNameEn, String categoryNameTr, Map<String, Object> attributes) {
    }
}
