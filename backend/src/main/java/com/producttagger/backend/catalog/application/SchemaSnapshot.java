package com.producttagger.backend.catalog.application;

import java.util.Map;

/**
 * Serializable snapshot of a category's active schema. Unlike the entity it
 * carries no lazy references or persistence state, so it is safe to cache
 * with any provider.
 */
public record SchemaSnapshot(String categoryCode, int version, Map<String, Object> schema) {
}
