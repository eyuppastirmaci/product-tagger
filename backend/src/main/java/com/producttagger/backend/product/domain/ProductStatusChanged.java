package com.producttagger.backend.product.domain;

import java.util.UUID;

/**
 * Fired on every lifecycle transition; feeds the SSE stream (not the outbox).
 */
public record ProductStatusChanged(UUID productId, ProductStatus status, boolean descriptionsReady) {
}
