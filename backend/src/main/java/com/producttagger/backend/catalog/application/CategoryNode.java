package com.producttagger.backend.catalog.application;

import java.util.List;

/**
 * Application-level view of the category tree, assembled from the flat
 * adjacency list.
 */
public record CategoryNode(
        String code,
        String nameTr,
        String nameEn,
        boolean leaf,
        List<CategoryNode> children) {
}
