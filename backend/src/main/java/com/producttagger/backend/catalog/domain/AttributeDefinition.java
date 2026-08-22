package com.producttagger.backend.catalog.domain;

import java.util.List;

/**
 * Typed view of one attribute in a category schema; values are the allowed
 * enum codes (empty for non-enum types).
 */
public record AttributeDefinition(
        String key,
        String type,
        boolean required,
        boolean multi,
        List<String> values) {
}
