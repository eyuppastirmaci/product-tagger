package com.producttagger.backend.product.api;

import com.producttagger.backend.product.domain.Product;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String status,
        String categoryCode,
        Map<String, Object> attributes,
        String originalImagePath,
        String processedImagePath,
        String thumbnailPath,
        String titleTr,
        String titleEn,
        String descriptionTr,
        String descriptionEn,
        Instant createdAt) {

    static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getStatus().name(),
                product.getCategory() != null ? product.getCategory().getCode() : null,
                product.getAttributes(),
                product.getImagePaths().getOriginal(),
                product.getImagePaths().getProcessed(),
                product.getImagePaths().getThumbnail(),
                product.getTitleTr(),
                product.getTitleEn(),
                product.getDescriptionTr(),
                product.getDescriptionEn(),
                product.getCreatedAt());
    }
}
