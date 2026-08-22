package com.producttagger.backend.catalog.api;

import com.producttagger.backend.catalog.application.CategoryNode;

import java.util.List;

public record CategoryTreeResponse(
        String code,
        String nameTr,
        String nameEn,
        boolean leaf,
        List<CategoryTreeResponse> children) {

    static CategoryTreeResponse from(CategoryNode node) {
        return new CategoryTreeResponse(
                node.code(),
                node.nameTr(),
                node.nameEn(),
                node.leaf(),
                node.children().stream().map(CategoryTreeResponse::from).toList());
    }
}
