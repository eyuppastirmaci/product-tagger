package com.producttagger.backend.product.api;

public record UpdateContentRequest(
        String titleTr,
        String titleEn,
        String descriptionTr,
        String descriptionEn) {
}
