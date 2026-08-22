package com.producttagger.backend.product.domain;

public enum ProductStatus {
    UPLOADED,
    PREPROCESSED,
    TAGGING,
    PENDING_REVIEW,
    APPROVED,
    FAILED
}
