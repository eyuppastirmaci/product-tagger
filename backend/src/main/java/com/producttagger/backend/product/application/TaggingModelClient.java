package com.producttagger.backend.product.application;

import com.producttagger.backend.catalog.domain.AttributeDefinition;

import java.util.List;
import java.util.Map;

/**
 * Port for the vision model behind the tagging pipeline; the Spring AI
 * implementation serves both the hosted and local profiles.
 */
public interface TaggingModelClient {

    /**
     * Asks the model to pick one option (or "other") for the product photo.
     * Used once per level of the category descent.
     */
    CategoryChoice pickCategory(byte[] image, List<CategoryOption> options);

    AttributeExtraction extractAttributes(byte[] image, List<AttributeDefinition> definitions);

    String OTHER = "other";

    record CategoryOption(String code, String name) {
    }

    record CategoryChoice(String code, double confidence, String modelName) {

        public boolean isOther() {
            return OTHER.equalsIgnoreCase(code);
        }
    }

    record AttributeExtraction(Map<String, Object> attributes,
                               Map<String, Double> confidences,
                               String titleTr,
                               String titleEn,
                               String modelName) {
    }
}
