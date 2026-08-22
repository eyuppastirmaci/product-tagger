package com.producttagger.backend.catalog.application;

/**
 * Cache names of the catalog context. Values put into these caches must stay
 * serializable records so a future Redis provider can store them as-is.
 */
public final class CatalogCaches {

    public static final String CATEGORY_TREE = "catalog.category-tree";
    public static final String LEAF_SCHEMA = "catalog.leaf-schema";

    private CatalogCaches() {
    }
}
