package com.producttagger.backend.product.application;

import java.util.Map;

/**
 * Port for bilingual title and description generation from approved attributes;
 * the text is derived from structured data only, never from the image.
 */
public interface DescriptionModelClient {

    GeneratedContent generate(String categoryNameEn, String categoryNameTr, Map<String, Object> attributes);

    record GeneratedContent(String titleTr, String titleEn, String descriptionTr, String descriptionEn) {
    }
}
