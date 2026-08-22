package com.producttagger.backend.product.api;

import com.producttagger.backend.catalog.domain.Category;
import com.producttagger.backend.product.domain.Product;
import com.producttagger.backend.product.domain.TagRevision;
import com.producttagger.backend.product.domain.TagRevisionSource;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        String status,
        String processedImagePath,
        String thumbnailPath,
        String titleTr,
        String titleEn,
        String descriptionTr,
        String descriptionEn,
        Instant createdAt,
        Proposal proposal) {

    public record Proposal(
            int revisionNo,
            CategoryRef proposedCategory,
            Map<String, Object> attributes,
            Map<String, Object> confidences,
            String modelName,
            Instant createdAt) {
    }

    public record CategoryRef(String code, String nameTr, String nameEn) {

        static CategoryRef from(Category category) {
            return category == null
                    ? null
                    : new CategoryRef(category.getCode(), category.getNameTr(), category.getNameEn());
        }
    }

    public static ReviewResponse from(Product product) {
        return new ReviewResponse(
                product.getId(),
                product.getStatus().name(),
                product.getImagePaths().getProcessed(),
                product.getImagePaths().getThumbnail(),
                product.getTitleTr(),
                product.getTitleEn(),
                product.getDescriptionTr(),
                product.getDescriptionEn(),
                product.getCreatedAt(),
                latestAiProposal(product.getRevisions()));
    }

    // A FAILED product may have no AI revision at all; the proposal is then null
    private static Proposal latestAiProposal(List<TagRevision> revisions) {
        return revisions.stream()
                .filter(revision -> revision.getSource() == TagRevisionSource.AI)
                .reduce((first, second) -> second)
                .map(revision -> new Proposal(
                        revision.getRevisionNo(),
                        CategoryRef.from(revision.getProposedCategory()),
                        revision.getProposedAttributes(),
                        revision.getConfidences(),
                        revision.getModelName(),
                        revision.getCreatedAt()))
                .orElse(null);
    }
}
