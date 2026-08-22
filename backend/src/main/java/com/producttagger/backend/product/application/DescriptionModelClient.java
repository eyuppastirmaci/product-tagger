package com.producttagger.backend.product.application;

import java.util.Map;

/**
 * Port for bilingual description generation from approved attributes; the text
 * is derived from structured data only, never from the image.
 */
public interface DescriptionModelClient {

    Descriptions generate(String categoryNameEn, String categoryNameTr, Map<String, Object> attributes);

    record Descriptions(String tr, String en) {
    }
}
